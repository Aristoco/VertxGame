package com.aristoco.core.guice.listener;

import cn.hutool.core.collection.CollectionUtil;
import com.aristoco.core.GameApplicationContext;
import com.aristoco.core.GameApplicationContextFactory;
import com.aristoco.core.bean.definition.BeanClassDefinition;
import com.aristoco.core.bean.definition.ValueConfigBindBeanClassDefinition;
import com.aristoco.core.utils.MvelUtils;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import io.vertx.core.json.JsonObject;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * @author chenguowei
 * @date 2024/3/8
 * @description PostConstruct初始化和配置注入属性监听器
 **/
public class PostConstructAndValueFiledInjectionListener implements TypeListener {

    private final GameApplicationContext context;

    public PostConstructAndValueFiledInjectionListener(GameApplicationContext context) {
        this.context = context;
    }

    /**
     * 依赖注入时监听
     *
     * @param type      encountered by Guice
     * @param encounter context of this encounter, enables reporting errors, registering injection
     *                  listeners and binding method interceptors for {@code type}.
     * @param <I>
     */
    @Override
    public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
        Class<?> rawType = type.getRawType();
        //优先进行属性注入
        Map<Class<?>, List<ValueConfigBindBeanClassDefinition>> configBindKeyMap = context.getConfigBindKeyMap();
        Map<Class<?>, List<BeanClassDefinition>> postConstructMethodMap = context.getPostConstructMethodMap();
        if (configBindKeyMap.containsKey(rawType)) {
            JsonObject configJson = GameApplicationContextFactory.getBootstrapApplicationContext()
                    .getApplicationProfileConfigJson();
            Map<String, Object> configJsonMap = configJson.getMap();
            List<ValueConfigBindBeanClassDefinition> beanClassDefinitions = configBindKeyMap.get(rawType);
            encounter.register((InjectionListener<I>) instance -> {
                for (ValueConfigBindBeanClassDefinition beanClassDefinition : beanClassDefinitions) {
                    Class<?> filedClass = beanClassDefinition.getFiledClass();
                    Field field = beanClassDefinition.getField();
                    Serializable expression = beanClassDefinition.getExpression();
                    Object object = MvelUtils.handleExpression(expression, configJsonMap, filedClass);
                    //使用反射
                    try {
                        field.set(instance, object);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                //执行初始化方法,保证在属性注入后
                List<BeanClassDefinition> postConstructMethodL = postConstructMethodMap.get(rawType);
                if (CollectionUtil.isNotEmpty(postConstructMethodL)) {
                    handlePostConstructMethod(postConstructMethodL, instance);
                }
            });
        } else {
            //执行初始化方法,没有要注入的属性
            List<BeanClassDefinition> postConstructMethodL = postConstructMethodMap.get(rawType);
            if (CollectionUtil.isNotEmpty(postConstructMethodL)) {
                encounter.register((InjectionListener<I>) instance ->
                        handlePostConstructMethod(postConstructMethodL, instance));
            }
        }
    }

    /**
     * 执行初始化方法
     *
     * @param postConstructMethodL 初始化方法列表
     * @param instance             要执行初始化的实例
     * @param <I>                  实例泛型
     */
    private <I> void handlePostConstructMethod(List<BeanClassDefinition> postConstructMethodL, I instance) {
        postConstructMethodL.forEach(beanClassDefinition -> {
            Method method = beanClassDefinition.getMethod();
            try {
                method.invoke(instance);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

}
