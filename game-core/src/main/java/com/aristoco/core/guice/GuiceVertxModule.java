/*
 * The MIT License (MIT)
 * Copyright © 2016 Englishtown <opensource@englishtown.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.aristoco.core.guice;

import cn.hutool.core.util.StrUtil;
import com.aristoco.core.BootstrapApplicationContext;
import com.aristoco.core.BootstrapVerticle;
import com.aristoco.core.GameApplicationContext;
import com.aristoco.core.GameApplicationContextFactory;
import com.aristoco.core.bean.definition.*;
import com.aristoco.core.config.VerticleBaseConfig;
import com.aristoco.core.event.ApplicationEventMulticaster;
import com.aristoco.core.exception.GameApplicationContextNotInitException;
import com.aristoco.core.guice.listener.PostConstructAndValueFiledInjectionListener;
import com.aristoco.core.guice.provider.ConfigurationBeanClassProvider;
import com.aristoco.core.utils.MvelUtils;
import com.aristoco.core.utils.StringUtils;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.SharedData;
import org.mvel2.PropertyAccessException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guice {@link AbstractModule} for vertx and container injections
 *
 * @author Administrator
 */
public class GuiceVertxModule extends AbstractModule {

    /**
     * 当前vertx实例
     */
    private final Vertx vertx;

    /**
     * 当前应用上下文
     */
    private final GameApplicationContext context;

    /**
     * provider缓存
     */
    private Map<Class<?>, Provider<?>> providerMap = new HashMap<>();

    public GuiceVertxModule(Vertx vertx, GameApplicationContext context) {
        this.vertx = vertx;
        this.context = context;
    }

    /**
     * guice绑定配置
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        //获取应用上下文
        if (!context.isInit()) {
            //未正常启动
            throw new GameApplicationContextNotInitException();
        }
        //上下文注入vertx对象
        context.setVertx(vertx);
        //绑定上下文信息
        bind(GameApplicationContext.class).toInstance(context);

        //监听注解，执行配置值注入和初始化方法
        bindListener(Matchers.any(), new PostConstructAndValueFiledInjectionListener(context));

        //绑定当前vertx实例
        bind(Vertx.class).toInstance(vertx);
        //绑定eventbus方便使用
        bind(EventBus.class).toInstance(vertx.eventBus());
        //绑定SharedData方便使用
        bind(SharedData.class).toInstance(vertx.sharedData());

        //处理启动时扫描分析出的依赖加载
        //绑定所有单例组件
        bindComponent(context);

        //绑定所有接口和实现类列表,比如通过接口多注入[Set<?>],通过指定实现类名注入【@Named】
        bindInterfaceClass(context);

        //绑定所有的子类和父类
        bindExtendClass(context);

        //获取provider绑定
        bindProviderClass(context);

        //配置绑定类和verticle配置注入关联
        bindConfigPropertiesClassAndVerticleConfigClass(context);

        //配置类注入管理
        bindConfigurationInjectClass(context);

        //verticle多绑定管理
        bindVerticleClass(context);

        //注册事件多播器
        bindEventMulticaster(context);

        //清除注入时使用的缓存
        clearInjectCache();
    }

    /**
     * 清理注入时的缓存
     */
    private void clearInjectCache() {
        this.providerMap = null;
    }

    /**
     * 绑定事件多播器
     *
     * @param context
     */
    private void bindEventMulticaster(GameApplicationContext context) {
        ApplicationEventMulticasterClassDefinition eventMulticasterDefinition = context.getApplicationEventMulticasterDefinition();
        Class<? extends ApplicationEventMulticaster> clazz = eventMulticasterDefinition.getClazz();
        String beanName = eventMulticasterDefinition.getBeanName();
        createBindingForExtend(beanName, ApplicationEventMulticaster.class, clazz,
                eventMulticasterDefinition.isSingleton());
    }

    /**
     * verticle多绑定管理,便于统一注入
     *
     * @param context
     */
    private void bindVerticleClass(GameApplicationContext context) {
        if (context instanceof BootstrapApplicationContext bootstrapApplicationContext) {
            Multibinder<AbstractVerticle> multibinder = Multibinder.newSetBinder(binder(), AbstractVerticle.class);
            TreeMap<Integer, List<BaseVerticleClassDefinition>> verticleClassTreeMap = bootstrapApplicationContext.getVerticleClassTreeMap();
            verticleClassTreeMap.values()
                    .forEach(abstractVerticleClassDefinitions -> {
                        abstractVerticleClassDefinitions.forEach(baseVerticleClassDefinition -> {
                            Class<? extends AbstractVerticle> verticleClazz = baseVerticleClassDefinition.getClazz();
                            //排除引导verticle，防止循环依赖
                            if (BootstrapVerticle.class.isAssignableFrom(verticleClazz)) {
                                return;
                            }
                            ScopedBindingBuilder scopedBindingBuilder = multibinder.addBinding().to(verticleClazz);
                            if (baseVerticleClassDefinition.isSingleton()) {
                                scopedBindingBuilder.in(Singleton.class);
                            }
                        });
                    });
        }
    }

    /**
     * 配置类注入管理
     *
     * @param context
     */
    private void bindConfigurationInjectClass(GameApplicationContext context) {
        Map<Class<?>, List<ConfigurationBeanClassDefinition>> configurationClassMap = context.getConfigurationClassMap();
        configurationClassMap.values()
                .forEach(configurationClass -> configurationClass
                        .forEach(beanClassDefinition -> createBeanClassProvider(beanClassDefinition.getBeanName(),
                                beanClassDefinition.getClazz(), beanClassDefinition)));
    }

    /**
     * 绑定配置绑定类和verticle配置类
     *
     * @param context 应用上下文
     */
    private void bindConfigPropertiesClassAndVerticleConfigClass(GameApplicationContext context) {
        //配置绑定类处理,特殊处理部署verticle的配置
        JsonObject configJson = GameApplicationContextFactory.getBootstrapApplicationContext()
                .getApplicationProfileConfigJson();
        Map<String, Object> configJsonMap = configJson.getMap();
        Map<String, VerticleBaseConfig> verticleBaseConfigMap = new HashMap<>();
        Map<String, BeanClassDefinition> configPropertityClassMap = context.getConfigPropertityClassMap();
        configPropertityClassMap.forEach((prefix, classDefinition) -> {
            //解析mvel表达式，配置类都是对象
            JsonObject configPropertiesJson;
            try {
                configPropertiesJson = MvelUtils.handleExpression("${" + prefix + "}",
                        configJsonMap, JsonObject.class);
                if (configPropertiesJson == null) {
                    configPropertiesJson = JsonObject.of();
                }
            } catch (PropertyAccessException e) {
                //找不到返回指定对象,创建一个默认对象
                configPropertiesJson = JsonObject.of();
            }
            AnnotatedBindingBuilder<?> bind = bind(classDefinition.getClazz());
            if (StrUtil.isNotBlank(classDefinition.getBeanName())) {
                bind.annotatedWith(Names.named(classDefinition.getBeanName()));
            }
            bind.toInstance(getConfigPropertiesObject(classDefinition, configPropertiesJson, verticleBaseConfigMap));
        });

        //所有部署配置统一部署,通过map注入
        MapBinder<String, VerticleBaseConfig> mapBinder = MapBinder.newMapBinder(binder(), String.class, VerticleBaseConfig.class);
        verticleBaseConfigMap.forEach((name, verticleBaseConfig) -> {
            if (StrUtil.isBlank(name)) {
                name = StringUtils.toLowerCaseCamel(verticleBaseConfig.getClass().getSimpleName());
            }
            mapBinder.addBinding(name).toInstance(verticleBaseConfig);
        });
    }

    /**
     * 获取配置类对象
     *
     * @param <T>                   解决guice错误
     * @param beanClassDefinition   配置类对象定义
     * @param configPropertiesJson  配置类对象json
     * @param verticleBaseConfigMap
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> T getConfigPropertiesObject(BeanClassDefinition beanClassDefinition, JsonObject configPropertiesJson,
                                            Map<String, VerticleBaseConfig> verticleBaseConfigMap) {
        Object object = configPropertiesJson.mapTo(beanClassDefinition.getClazz());
        //判断是否是部署配置
        if (VerticleBaseConfig.class.isAssignableFrom(beanClassDefinition.getClazz())) {
            verticleBaseConfigMap.put(beanClassDefinition.getBeanName(), (VerticleBaseConfig) object);
        }
        return (T) object;
    }

    /**
     * 绑定Provider提供的类
     *
     * @param context
     */
    @SuppressWarnings("unchecked")
    private void bindProviderClass(GameApplicationContext context) {
        Map<Class<?>, ProviderClassDefinition> providerClassMap = context.getProviderClassMap();
        providerClassMap.forEach((clazz, classDefinition) -> createProviderBindingForClass((Class<Object>) clazz,
                (Class<? extends Provider<Object>>) classDefinition.getClazz(),
                classDefinition.getBeanName(), classDefinition.isSingleton()));
    }

    /**
     * 绑定所有接口和实现类列表
     *
     * @param context
     */
    @SuppressWarnings("unchecked")
    private void bindInterfaceClass(GameApplicationContext context) {
        Map<Class<?>, Map<String, BeanClassDefinition>> interfaceClassMap = context.getInterfaceClassMap();
        interfaceClassMap.forEach((interfaceClass, nameMap) -> {
            // 所有映射值都是正确的实现类类型，转换为泛型集合
            Map<Class<?>, BeanClassDefinition> implementationMap = nameMap.values()
                    .stream()
                    .collect(Collectors.toMap(BeanClassDefinition::getClazz,
                            Function.identity(), (k1, k2) -> k1));
            createMultiBinding((Class<Object>) interfaceClass, implementationMap);

            //接口绑定
            if (nameMap.size() == 1) {
                //只有一个直接默认绑定
                nameMap.values()
                        .forEach(beanClassDefinition -> {
                            if (beanClassDefinition instanceof ConfigurationBeanClassDefinition configurationBeanClassDefinition) {
                                createBeanClassProvider(null, interfaceClass, configurationBeanClassDefinition);
                            } else {
                                createBindingForInterface(null, (Class<Object>) interfaceClass,
                                        beanClassDefinition.getClazz(), beanClassDefinition.isSingleton());
                            }
                        });
            } else {
                nameMap.forEach((name, beanClassDefinition) -> {
                    if (beanClassDefinition instanceof ConfigurationBeanClassDefinition configurationBeanClassDefinition) {
                        //如果是主要的提供默认绑定
                        if (configurationBeanClassDefinition.isPrimary()) {
                            createBeanClassProvider(null, interfaceClass, configurationBeanClassDefinition);
                        }
                        createBeanClassProvider(name, interfaceClass, configurationBeanClassDefinition);
                    } else {
                        //如果是主要的提供默认绑定
                        if (beanClassDefinition.isPrimary()) {
                            createBindingForInterface(null, (Class<Object>) interfaceClass,
                                    beanClassDefinition.getClazz(), beanClassDefinition.isSingleton());
                        }
                        //绑定接口到实现使用指定名
                        createBindingForInterface(name, (Class<Object>) interfaceClass,
                                beanClassDefinition.getClazz(), beanClassDefinition.isSingleton());
                    }
                });
            }
        });
    }

    /**
     * 绑定所有子类和父类列表
     *
     * @param context
     */
    @SuppressWarnings("unchecked")
    private void bindExtendClass(GameApplicationContext context) {
        Map<Class<?>, Map<String, BeanClassDefinition>> extendClassMap = context.getExtendClassMap();
        extendClassMap.forEach((supreClass, nameMap) -> {
            // 所有映射值都是正确的实现类类型，转换为泛型集合
            Map<Class<?>, BeanClassDefinition> extendMap = nameMap.values()
                    .stream()
                    .collect(Collectors.toMap(BeanClassDefinition::getClazz,
                            Function.identity(), (k1, k2) -> k1));
            // 调用泛型方法，注意这里使用了原始类型来避免类型擦除警告，但这不是一个类型安全的做法
            createMultiBinding((Class<Object>) supreClass, extendMap);

            //实现类绑定
            if (nameMap.size() == 1) {
                //只有一个直接默认绑定
                nameMap.values()
                        .forEach(beanClassDefinition -> {
                                    if (beanClassDefinition instanceof ConfigurationBeanClassDefinition configurationBeanClassDefinition) {
                                        createBeanClassProvider(null, supreClass, configurationBeanClassDefinition);
                                    } else {
                                        createBindingForExtend(null, (Class<Object>) supreClass,
                                                beanClassDefinition.getClazz(), beanClassDefinition.isSingleton());
                                    }
                                }
                        );
            } else {
                nameMap.forEach((name, beanClassDefinition) -> {
                    if (beanClassDefinition instanceof ConfigurationBeanClassDefinition configurationBeanClassDefinition) {
                        //如果是主要的提供默认绑定
                        if (beanClassDefinition.isPrimary()) {
                            createBeanClassProvider(null, supreClass, configurationBeanClassDefinition);
                        }
                        createBeanClassProvider(name, supreClass, configurationBeanClassDefinition);
                    } else {
                        //如果是主要的提供默认绑定
                        if (beanClassDefinition.isPrimary()) {
                            createBindingForExtend(null, (Class<Object>) supreClass,
                                    beanClassDefinition.getClazz(), beanClassDefinition.isSingleton());
                        }
                        //绑定接口到实现使用指定名
                        createBindingForExtend(name, (Class<Object>) supreClass,
                                beanClassDefinition.getClazz(), beanClassDefinition.isSingleton());
                    }
                });
            }
        });
    }

    /**
     * 绑定单例组件
     *
     * @param context
     */
    private void bindComponent(GameApplicationContext context) {
        Set<BeanClassDefinition> singletonClassSet = context.getClassSet();
        singletonClassSet.forEach(beanClassDefinition -> {
            createSimpleBinding(beanClassDefinition.getBeanName(), beanClassDefinition.getClazz(),
                    beanClassDefinition.isSingleton());
        });
    }


    /**
     * 创建Provider绑定
     *
     * @param <T>           用于消除guice的错误
     * @param clazz         提供的类
     * @param providerClass 复杂实例化的提供类
     * @param beanName      指定名称
     * @param singleton     是否是单例
     */
    public <T> void createProviderBindingForClass(Class<T> clazz, Class<? extends Provider<T>> providerClass,
                                                  String beanName, boolean singleton) {
        AnnotatedBindingBuilder<T> bind = bind(clazz);
        if (StrUtil.isNotBlank(beanName)) {
            bind.annotatedWith(Names.named(beanName));
        }
        bind.toProvider(providerClass);
        if (singleton) {
            bind.in(Singleton.class);
        }
    }

    /**
     * 创建多绑定
     *
     * @param clazz         绑定的接口/父类
     * @param multiClassMap 绑定的实现类map/子类
     * @param <T>           用于消除guice的错误
     */
    @SuppressWarnings("unchecked")
    public <T> void createMultiBinding(Class<T> clazz,
                                       Map<Class<? extends T>, BeanClassDefinition> multiClassMap) {
        Multibinder<T> multibinder = Multibinder.newSetBinder(binder(), clazz);
        multiClassMap.forEach((multiClass, beanclassDefinition) -> {
            ScopedBindingBuilder scopedBindingBuilder;
            if (beanclassDefinition instanceof ConfigurationBeanClassDefinition configurationBeanClassDefinition) {
                Provider<?> provider = providerMap.computeIfAbsent(configurationBeanClassDefinition.getClazz(),
                        k -> new ConfigurationBeanClassProvider<>(context, configurationBeanClassDefinition));
                scopedBindingBuilder = multibinder.addBinding().toProvider((Provider<? extends T>) provider);
            } else {
                scopedBindingBuilder = multibinder.addBinding().to(multiClass);
            }
            if (beanclassDefinition.isSingleton()) {
                scopedBindingBuilder.in(Singleton.class);
            }
        });
    }

    /**
     * 创建接口命名绑定
     *
     * @param <T>                 用于消除guice的错误
     * @param name                接口名【未自定义就是接口名首字母小写】
     * @param interfaceClass      接口
     * @param implementationClass 实现类
     * @param singleton           是否是单例
     */
    public <T> void createBindingForInterface(String name, Class<T> interfaceClass, Class<? extends T> implementationClass, boolean singleton) {
        AnnotatedBindingBuilder<T> bind = bind(interfaceClass);
        if (StringUtils.isNotBlank(name)) {
            bind.annotatedWith(Names.named(name));
        }
        bind.to(implementationClass);
        if (singleton) {
            bind.in(Singleton.class);
        }
    }

    /**
     * Bean注解产生的依赖类
     *
     * @param clazz               需要管理的类
     * @param beanClassDefinition 类的定义信息
     * @param <T>                 用于消除guice的错误
     */
    @SuppressWarnings("unchecked")
    public <T> void createBeanClassProvider(String beanName, Class<T> clazz, ConfigurationBeanClassDefinition beanClassDefinition) {
        AnnotatedBindingBuilder<T> bind = bind(clazz);
        //bean名字,如果自定义名字那就需要再注入时使用@Named
        if (StrUtil.isNotBlank(beanName)) {
            bind.annotatedWith(Names.named(beanName));
        }
        Provider<?> provider = providerMap.computeIfAbsent(beanClassDefinition.getClazz(),
                k -> new ConfigurationBeanClassProvider<>(context, beanClassDefinition));
        bind.toProvider((Provider<? extends T>) provider);
        //是否是单例
        if (beanClassDefinition.isSingleton()) {
            bind.in(Singleton.class);
        }
    }

    /**
     * 创建简单命名绑定
     *
     * @param <T>       用于消除guice的错误
     * @param name      bean名字
     * @param clazz     类
     * @param singleton 是否是单例
     */
    public <T> void createSimpleBinding(String name, Class<T> clazz, boolean singleton) {
        AnnotatedBindingBuilder<T> bind = bind(clazz);
        if (StrUtil.isNotBlank(name)) {
            bind.annotatedWith(Names.named(name))
                    .to(clazz);
        }
        if (singleton) {
            bind.in(Singleton.class);
        }
    }

    /**
     * 创建继承命名绑定
     *
     * @param <T>        用于消除guice的错误
     * @param name       bean名字
     * @param superClazz 父类
     * @param clazz      子类
     * @param singleton  是否是单例
     */
    public <T> void createBindingForExtend(String name, Class<T> superClazz, Class<? extends T> clazz, boolean singleton) {
        AnnotatedBindingBuilder<T> bind = bind(superClazz);
        if (StrUtil.isNotBlank(name)) {
            bind.annotatedWith(Names.named(name));
        }
        bind.to(clazz);
        if (singleton) {
            bind.in(Singleton.class);
        }
    }

}

