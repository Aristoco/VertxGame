package com.aristoco.core;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.aristoco.core.annotation.*;
import com.aristoco.core.bean.definition.*;
import com.aristoco.core.config.ApplicationConfig;
import com.aristoco.core.config.CustomConfigStoreOptions;
import com.aristoco.core.config.VerticleBaseConfig;
import com.aristoco.core.constant.GameApplicationConstants;
import com.aristoco.core.event.ApplicationEventMulticaster;
import com.aristoco.core.event.ApplicationListener;
import com.aristoco.core.event.EventBusApplicationEventMulticaster;
import com.aristoco.core.event.PayloadApplicationEvent;
import com.aristoco.core.event.annotation.EventListener;
import com.aristoco.core.exception.*;
import com.aristoco.core.guice.GuiceVerticleFactory;
import com.aristoco.core.jackson.GameJacksonConfig;
import com.aristoco.core.jackson.JsonArrayDeserializer;
import com.aristoco.core.jackson.JsonObjectDeserializer;
import com.aristoco.core.utils.MvelUtils;
import com.aristoco.core.utils.ReflectionUtils;
import com.aristoco.core.utils.StringUtils;
import com.aristoco.core.utils.TimeUtils;
import com.aristoco.core.vertx.VerticleDeployFailedHandle;
import com.aristoco.core.vertx.VertxAfterStopHandle;
import com.aristoco.core.vertx.verticle.BaseVerticle;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.google.inject.Module;
import com.google.inject.Provider;
import io.github.classgraph.*;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.json.jackson.VertxModule;
import jakarta.annotation.PostConstruct;
import lombok.Cleanup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.aristoco.core.constant.GameApplicationConstants.BASE_SCAN_PACKAGE_PATH;
import static com.aristoco.core.utils.ClassUtils.isPrimitiveOrString;

/**
 * @author chenguowei
 * @date 2024/6/13
 * @description
 **/
@Slf4j
public class GameApplication extends Launcher {

    /**
     * 默认激活的环境
     */
    private static final String DEFAULT_PROFILES_ACTIVE = null;

    /**
     * 不需要读取配置的环境
     */
    private static final String DEFAULT_PROFILES_ACTIVE_STOP = "STOP";

    /**
     * 配置简写后缀
     */
    private static final String CONFIG_YAML_SUFFIX_SIMPLE = ".yml";

    /**
     * 配置完整后缀
     */
    private static final String CONFIG_YAML_SUFFIX = ".yaml";

    /**
     * 低优先级配置路径
     */
    private static final String DEFAULT_CONFIG_PATH = "application";

    /**
     * 高优先级配置路径,相同的覆盖低优先级的
     */
    private static final String DEFAULT_CONFIG_PATH_PRIORITY = "conf" + File.separator + "application";

    /**
     * 启动类
     */
    private final Class<?> startClass;

    /**
     * 修改vertx启动自定义配置
     */
    private final Map<String, Object> vertxConfig = new HashMap<>();

    /**
     * 修改vertx启动配置
     */
    @Setter
    private Consumer<VertxOptions> updateVertxOptions;

    /**
     * vertx启动后修改
     */
    @Setter
    private Consumer<Vertx> customAfterStartingVertx;

    /**
     * vertical部署配置修改
     */
    @Setter
    private Consumer<DeploymentOptions> updateBeforeDeployingVerticleOptions;

    /**
     * vertx停止前处理
     */
    @Setter
    private Consumer<Vertx> updateBeforeStoppingVertx;

    /**
     * vertx停止后处理
     */
    @Setter
    private VertxAfterStopHandle vertxAfterStopHandle;

    /**
     * verticle部署失败处理
     */
    @Setter
    private VerticleDeployFailedHandle verticleDeployFailedHandle;

    /**
     * 自定义jackson配置
     */
    @Setter
    private GameJacksonConfig customGameJacksonConfig;

    /**
     * 额外的guice模块
     */
    private final List<Module> extraModules;

    /**
     * 默认启动类型
     *
     * @param startClass 启动类
     */
    public GameApplication(Class<?> startClass) {
        this(startClass, List.of());
    }

    /**
     * 自定义依赖启动类
     *
     * @param startClass  启动类
     * @param extraModule 额外的guice依赖管理模块
     */
    public GameApplication(Class<?> startClass, Module extraModule) {
        this(startClass, List.of(extraModule));
    }

    /**
     * 自定义依赖启动类
     *
     * @param startClass   启动类
     * @param extraModules 额外的guice依赖管理模块
     */
    public GameApplication(Class<?> startClass, List<Module> extraModules) {
        this.startClass = startClass;
        this.extraModules = extraModules;
    }

    /**
     * 主启动
     * <p> 1.扫描主启动类所在的目录或者子目录下的所有Verticle
     * <p> 2.然后根据不同的Verticle构建各自的Context
     * <p> 3.然后根据不同的Verticle扫描每个Verticle所在的目录和子目录中需要的类进行处理
     * <p> 4.根据每个Verticle进行类的依赖和各种初始化处理
     * <p> 5.每个Verticle启动自身，管理各自的类
     *
     * @param args
     */
    public void run(String[] args) {
        //定义当前jackson的模式
        configureJson();

        //扫描当前应用所需的类信息
        @Cleanup ScanResult scanResult = scanApplicationClassInfos();

        //初始化上下文信息
        ClassInfoList verticleClassInfoList = initGameApplicationContext(scanResult);

        //todo 配置添加是否要部署Verticle，否则是用于高可用的

        //处理需要部署的verticle
        handleDeployVerticle(verticleClassInfoList);

        //启动应用
        startApplication(args);

    }

    /**
     * 初始化上下文信息
     *
     * @param scanResult 类扫描结果
     * @param scanResult
     * @return 返回所有的Verticle类信息
     */
    @SuppressWarnings("unchecked")
    private ClassInfoList initGameApplicationContext(ScanResult scanResult) {
        //找到当前启动目录下的所有Verticle
        ClassInfoList verticleClassInfoList = scanResult.getClassesWithAnnotation(DeployVerticle.class.getName());
        verticleClassInfoList.forEach(classInfo -> {
            //判断是否是引导类
            boolean isBootstrapVerticle = isBootstrapVerticleClassInfo(classInfo);

            Class<? extends BaseVerticle> verticleClass = (Class<? extends BaseVerticle>) classInfo.loadClass();
            DeployVerticle deployVerticle = AnnotationUtil.getSynthesizedAnnotation(verticleClass, DeployVerticle.class);
            //获取Verticle特有的guice模块
            Class<Module>[] extraGuiceModule = deployVerticle.extraGuiceModule();
            registerGuiceModule(extraGuiceModule);

            //初始化上下文对象
            GameApplicationContext gameApplicationContext;
            if (isBootstrapVerticle) {
                gameApplicationContext = GameApplicationContextFactory.getBootstrapApplicationContext();
            } else {
                gameApplicationContext = GameApplicationContextFactory.createGameApplicationContext(verticleClass);
            }
            //添加额外的guice模块
            if (!extraModules.isEmpty()) {
                gameApplicationContext.getExtraGuiceModule().addAll(extraModules);
            }

            //获取对应的类过滤器
            ClassInfoList.ClassInfoFilter packageFilter;
            if (!isBootstrapVerticle) {
                //先白名单在黑名单过滤
                Set<String> excludeClassNameSet = Arrays.stream(deployVerticle.excludeClasses())
                        .map(Class::getName)
                        .collect(Collectors.toSet());
                Set<String> excludePackageNameSet = Arrays.stream(deployVerticle.excludePackageName())
                        .collect(Collectors.toSet());
                Set<String> includePackageNameSet = Arrays.stream(deployVerticle.includePackageName())
                        .collect(Collectors.toSet());
                Set<String> includeClassNameSet = Arrays.stream(deployVerticle.includePackageClasses())
                        .map(Class::getName)
                        .collect(Collectors.toSet());
                //添加当前Verticle路径
                includePackageNameSet.add(classInfo.getPackageInfo().getName());
                //先排除一下黑名单
                includeClassNameSet.removeAll(excludeClassNameSet);
                includePackageNameSet.removeAll(excludePackageNameSet);
                packageFilter = getClassInfoFilter(classInfo.getName(), includeClassNameSet, excludeClassNameSet,
                        includePackageNameSet, excludePackageNameSet);
            } else {
                //bootstrap单独去加载一些框架配置
                packageFilter = getBootstrapClassInfoFilter();
            }

            //处理需要依赖管理的类
            handleDependencyManagementClass(gameApplicationContext, scanResult, packageFilter);

        });
        return verticleClassInfoList;
    }

    /**
     * 获取Bootstrap类过滤器
     * @return 类过滤器
     */
    private ClassInfoList.ClassInfoFilter getBootstrapClassInfoFilter() {
        return classInfo -> {
            //1、框架包全放过
            String checkPackageName = classInfo.getPackageInfo().getName();
            boolean isInclude = checkPackageName.startsWith(BASE_SCAN_PACKAGE_PATH);
            if (isInclude) {
                return true;
            }
            //2、Verticle部署配置
            ClassInfo superclassInfo = classInfo.getSuperclass();
            if (superclassInfo != null && VerticleBaseConfig.class.getName().equals(superclassInfo.getName())) {
                return true;
            }
            //...其他框架需要的配置
            return false;
        };
    }

    /**
     * 判断是否是引导类的信息
     *
     * @param classInfo
     * @return
     */
    private boolean isBootstrapVerticleClassInfo(ClassInfo classInfo) {
        return classInfo.getName().equals(BootstrapVerticle.class.getName());
    }

    /**
     * 处理需要依赖管理的类
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param scanResult             类扫描结果
     * @param packageFilter          包过滤器
     */
    private void handleDependencyManagementClass(GameApplicationContext gameApplicationContext, ScanResult scanResult, ClassInfoList.ClassInfoFilter packageFilter) {
        //处理固定要注入的类
        handleFixedInjectClass(gameApplicationContext, scanResult, packageFilter);

        //处理配置类的注入
        handleConfigurationInjectClass(gameApplicationContext, scanResult, packageFilter);

        //处理属性配置值注入
        handleFiledConfigInjectValue(gameApplicationContext, scanResult, packageFilter);

        //处理初始化注解执行方法
        handlePostConstructMethod(gameApplicationContext, scanResult, packageFilter);

        //处理配置绑定类的注入
        handleConfigBindClassInject(gameApplicationContext, scanResult, packageFilter);

        //处理事件多播器
        handleEventMulticaster(gameApplicationContext, scanResult, packageFilter);

        //处理事件监听注解
        handleEventListenerAnnotation(gameApplicationContext, scanResult, packageFilter);

        //处理事件监听接口
        handleApplicationListenerInterface(gameApplicationContext, scanResult, packageFilter);
    }

    /**
     * 获取类过滤器
     * <p> 先检查白名单在检查黑名单，先检查路径在检查类
     *
     * @param verticleClassName     当前VerticleClass名字
     * @param includeClassNameSet   包含的类名集合
     * @param excludeClassNameSet   排除的类名集合
     * @param includePackageNameSet 包含的路径集合
     * @param excludePackageNameSet 排除的路径集合
     * @return 返回类过滤器
     */
    private ClassInfoList.ClassInfoFilter getClassInfoFilter(String verticleClassName, Set<String> includeClassNameSet,
                                                             Set<String> excludeClassNameSet,
                                                             Set<String> includePackageNameSet,
                                                             Set<String> excludePackageNameSet) {
        return classInfo -> {
            //排除非当前Verticle的类
            if (classInfo.hasAnnotation(DeployVerticle.class.getName())
                    && !classInfo.getName().equals(verticleClassName)) {
                return false;
            }
            String checkPackageName = classInfo.getPackageInfo().getName();
            String checkClassName = classInfo.getName();
            boolean isInclude = checkPackageName.startsWith(BASE_SCAN_PACKAGE_PATH);
            if (!isInclude && includePackageNameSet != null && !includePackageNameSet.isEmpty()) {
                for (String includePackageName : includePackageNameSet) {
                    if (checkPackageName.startsWith(includePackageName)) {
                        isInclude = true;
                        break;
                    }
                }
            }
            if (isInclude && excludePackageNameSet != null && !excludePackageNameSet.isEmpty()) {
                for (String excludePackageName : excludePackageNameSet) {
                    if (checkPackageName.startsWith(excludePackageName)) {
                        isInclude = false;
                        break;
                    }
                }
            }
            if (!isInclude && includeClassNameSet != null && !includeClassNameSet.isEmpty()) {
                isInclude = includeClassNameSet.contains(checkClassName);
            }
            if (isInclude && excludeClassNameSet != null && !excludeClassNameSet.isEmpty()) {
                isInclude = !excludeClassNameSet.contains(checkClassName);
            }


            return isInclude;
        };
    }

    /**
     * 处理事件监听接口
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param scanResult             类扫描结果
     * @param classInfoFilter        类过滤器
     */
    private void handleApplicationListenerInterface(GameApplicationContext gameApplicationContext, ScanResult scanResult,
                                                    ClassInfoList.ClassInfoFilter classInfoFilter) {
        scanResult.getClassesImplementing(ApplicationListener.class.getName())
                .filter(classInfoFilter)
                .filter(classInfo -> classInfo.hasAnnotation(Component.class.getName()))
                .filter(classInfo -> !classInfo.isAbstract())
                .filter(classInfo -> !classInfo.isInterface())
                .forEach(classInfo -> {
                    Class<?> clazz = classInfo.loadClass();
                    Component component = AnnotationUtil.getSynthesizedAnnotation(clazz, Component.class);
                    String beanName = component.value();
                    if (StringUtils.isBlank(beanName)) {
                        beanName = null;
                    }
                    //获取当前事件监听方法的参数名
                    MethodInfo methodInfo = classInfo.getMethodInfo("onApplicationEvent").get(0);
                    String parameterName = methodInfo.getParameterInfo()[0].getName();

                    //处理监听的类
                    for (Type genericInterface : clazz.getGenericInterfaces()) {
                        if (genericInterface instanceof ParameterizedType pt) {
                            if (!pt.getRawType().equals(ApplicationListener.class)) {
                                continue;
                            }
                            //监听器只会携带一个泛型，必定是要带泛型
                            ParameterizedType p = (ParameterizedType) pt.getActualTypeArguments()[0];
                            Class<?> eventClass;
                            Type rawType = p.getRawType();
                            if (rawType.getTypeName().equals(PayloadApplicationEvent.class.getName())) {
                                eventClass = (Class<?>) p.getActualTypeArguments()[0];
                            } else {
                                eventClass = (Class<?>) rawType;
                            }

                            EventListenerBeanClassDefinition beanClassDefinition = new EventListenerBeanClassDefinition();

                            beanClassDefinition.setListenerClasses(Set.of(eventClass));
                            beanClassDefinition.setPayloadEvent(true);
                            beanClassDefinition.setParameterName(parameterName);

                            beanClassDefinition.setClazz(clazz);
                            beanClassDefinition.setBeanName(beanName);

                            gameApplicationContext.getEventListeners().add(beanClassDefinition);
                            break;
                        }
                    }
                });
    }

    /**
     * 处理事件监听注解
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param scanResult             类扫描结果
     * @param classInfoFilter        类过滤器
     */
    private void handleEventListenerAnnotation(GameApplicationContext gameApplicationContext, ScanResult scanResult,
                                               ClassInfoList.ClassInfoFilter classInfoFilter) {
        scanResult.getClassesWithMethodAnnotation(EventListener.class.getName())
                .filter(classInfoFilter)
                .filter(classInfo -> !classInfo.isAbstract())
                .filter(classInfo -> !classInfo.isInterface())
                .filter(classInfo -> classInfo.hasAnnotation(Component.class.getName()))
                .forEach(classInfo -> {
                    Class<?> clazz = classInfo.loadClass();
                    Component component = AnnotationUtil.getSynthesizedAnnotation(clazz, Component.class);
                    classInfo.getMethodInfo()
                            .filter(methodInfo -> methodInfo.hasAnnotation(EventListener.class.getName()))
                            .forEach(methodInfo -> {
                                Method method = methodInfo.loadClassAndGetMethod();
                                //获取参数列表信息
                                Parameter[] parameters = method.getParameters();
                                //记录参数的位置
                                List<EventListenerBeanClassDefinition.ParameterBeanClassDefinition>
                                        parameterBeanClassDefinitions = Stream.of(parameters)
                                        .map(parameter -> {
                                            EventListenerBeanClassDefinition.ParameterBeanClassDefinition
                                                    parameterBeanClassDefinition =
                                                    new EventListenerBeanClassDefinition.ParameterBeanClassDefinition();
                                            Class<?> type = parameter.getType();
                                            String parameterName = parameter.getName();
                                            parameterBeanClassDefinition.setParameterClass(type);
                                            parameterBeanClassDefinition.setParameterName(parameterName);
                                            //判断是否是被payloadEvent包裹
                                            if (PayloadApplicationEvent.class.isAssignableFrom(type)) {
                                                Type genericType = parameter.getParameterizedType();
                                                if (genericType instanceof ParameterizedType pt) {
                                                    Type actualTypeArgument = pt.getActualTypeArguments()[0];
                                                    Class<?> typeClass = (Class<?>) actualTypeArgument;
                                                    parameterBeanClassDefinition.setParameterClass(typeClass);
                                                    parameterBeanClassDefinition.setPayloadEvent(true);
                                                }
                                            }
                                            return parameterBeanClassDefinition;
                                        })
                                        .collect(Collectors.toList());
                                //获取注解信息
                                EventListener eventListener = method.getAnnotation(EventListener.class);
                                String condition = eventListener.condition();
                                Class<?>[] eventSources = eventListener.eventSources();
                                Set<String> eventSourceSet = Stream.of(eventSources)
                                        .map(Class::getName)
                                        .collect(Collectors.toSet());
                                Class<?>[] listenerClasses = eventListener.value();
                                if (listenerClasses.length == 0) {
                                    listenerClasses = parameterBeanClassDefinitions.stream()
                                            .map(EventListenerBeanClassDefinition.ParameterBeanClassDefinition::getParameterClass)
                                            .toArray(Class<?>[]::new);
                                }
                                if (listenerClasses.length == 0) {
                                    log.warn("当前事件监听方法：{},没有监听任何事件", methodInfo.getName());
                                    return;
                                }

                                //记录事件监听器的类定义信息
                                EventListenerBeanClassDefinition beanClassDefinition = new EventListenerBeanClassDefinition();

                                beanClassDefinition.setListenerClasses(Stream.of(listenerClasses)
                                        .collect(Collectors.toSet()));
                                beanClassDefinition.setParameterBeanClassDefinitions(parameterBeanClassDefinitions);
                                beanClassDefinition.setAlone(eventListener.alone());
                                beanClassDefinition.setLocal(eventListener.local());
                                beanClassDefinition.setCondition(condition);
                                beanClassDefinition.setEventSources(eventSourceSet);

                                beanClassDefinition.setClazz(clazz);
                                beanClassDefinition.setBeanName(component.value());

                                ReflectionUtils.makeAccessible(method);
                                beanClassDefinition.setMethod(method);

                                gameApplicationContext.getEventListeners().add(beanClassDefinition);
                            });
                });
    }

    /**
     * 查找事件多播器的注入
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param scanResult             类扫描结果
     * @param classInfoFilter        类过滤器
     */
    @SuppressWarnings("unchecked")
    private void handleEventMulticaster(GameApplicationContext gameApplicationContext, ScanResult scanResult,
                                        ClassInfoList.ClassInfoFilter classInfoFilter) {
        //判断是否注入自定义事件多播器,框架自己的
        ClassInfoList eventMultiCasterL = scanResult.getClassesImplementing(ApplicationEventMulticaster.class.getName())
                .filter(classInfoFilter)
                .filter(classInfo -> !classInfo.isAbstract())
                .filter(classInfo -> !classInfo.isInterface())
                .filter(classInfo -> classInfo.hasAnnotation(Component.class.getName()))
                .filter(classInfo -> {
                    Class<?> clazz = classInfo.loadClass();
                    Component component = AnnotationUtil.getSynthesizedAnnotation(clazz, Component.class);
                    //判段是否注入的自定义的事件多播器
                    return GameApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME.equals(component.value());
                });
        ApplicationEventMulticasterClassDefinition eventMulticasterDefinition = gameApplicationContext.getApplicationEventMulticasterDefinition();
        eventMulticasterDefinition.setBeanName(GameApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
        if (eventMultiCasterL.isEmpty()) {
            eventMulticasterDefinition.setClazz(EventBusApplicationEventMulticaster.class);
        } else {
            if (eventMultiCasterL.size() > 1) {
                log.warn("事件多播器数量大于1个,将会选取首个多播器注册使用,当前事件多播器数量：{}", eventMultiCasterL.size());
            }
            eventMulticasterDefinition.setClazz((Class<? extends ApplicationEventMulticaster>) eventMultiCasterL.get(0).loadClass());
        }
    }

    /**
     * 启动应用
     *
     * @param args 外部参数
     */
    private void startApplication(String[] args) {
        //读取vertx启动配置，包含BootstrapVerticle的配置
        BootstrapApplicationContext bootstrapApplicationContext =
                GameApplicationContextFactory.getBootstrapApplicationContext();
        //创建一个临时vertx
        Vertx vertx = Vertx.vertx();
        //加载应用配置
        loadApplicationProfileConfig(vertx, DEFAULT_PROFILES_ACTIVE, null)
                .compose(configJson -> {
                    if (configJson == null) {
                        throw new VertxStartUpConfigNotFoundException("没有启动配置,请检查启动配置名字和路径是否正确");
                    }
                    //环境配置首次读取生效
                    ApplicationConfig tempConfig = configJson
                            .getJsonObject(GameApplicationConstants.GAME_APPLICATION_CONFIG_PREFIX)
                            .mapTo(ApplicationConfig.class);
                    String profile = tempConfig.getProfiles().getActive();
                    if (StrUtil.isBlank(profile)) {
                        profile = DEFAULT_PROFILES_ACTIVE_STOP;
                    }
                    //获取而外的配置中心配置
                    List<CustomConfigStoreOptions> customConfigStoreOptions = tempConfig.getConfigStoreOptions();
                    log.info("服务器启动，当前环境：{}", profile);
                    //加载环境配置,并且合并覆盖
                    return loadApplicationProfileConfig(vertx, profile, customConfigStoreOptions)
                            .compose(profileConfigJson -> {
                                if (profileConfigJson == null) {
                                    throw new VertxStartUpConfigNotFoundException("没有环境启动配置,请检查环境启动配置名字和路径是否正确");
                                }
                                //合并环境配置到应用配置中,便于某些基础配置也不同
                                JsonObject resultConfigJson = configJson.mergeIn(profileConfigJson, true);
                                ApplicationConfig applicationConfig = resultConfigJson
                                        .getJsonObject(GameApplicationConstants.GAME_APPLICATION_CONFIG_PREFIX)
                                        .mapTo(ApplicationConfig.class);
                                //获取当前的设置的时区,未设置则使用默认的【注意：如果服务器设计时间相关处理，需要保证每次部署时区的一致或者全部使用时间戳】
                                Integer timeOffset = applicationConfig.getTimeZone().getTimeOffset();
                                if (timeOffset != null && timeOffset >= -12 && timeOffset <= 12) {
                                    TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.ofHours(timeOffset)));
                                }
                                //初始化并打印
                                log.info("当前服务器启动时区: {}", TimeUtils.getUtcOffsetHourStr());
                                bootstrapApplicationContext.setApplicationConfig(applicationConfig);
                                bootstrapApplicationContext.setApplicationProfileConfigJson(resultConfigJson);
                                return Future.succeededFuture();
                            });
                })
                .compose(ignore -> {
                    //关闭临时的vertx,后续不再使用
                    vertx.close(ar -> {
                        //上下文初始化完成
                        GameApplicationContextFactory.applicationContextInitFinish();
                        //部署引导verticle
                        String[] bootstrapArgs = {"run", GuiceVerticleFactory.getGuiceVerticleName(BootstrapVerticle.class)};
                        dispatch(ArrayUtil.append(bootstrapArgs, args));
                    });
                    return Future.succeededFuture();
                })
                .onFailure(err -> {
                    log.error("服务启动出错！！！", err);
                    System.exit(0);
                });
    }

    /**
     * 处理配置绑定类的注入
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param scanResult             类扫描结果
     * @param classInfoFilter        类过滤器
     */
    private void handleConfigBindClassInject(GameApplicationContext gameApplicationContext, ScanResult scanResult,
                                             ClassInfoList.ClassInfoFilter classInfoFilter) {
        //查询所有的配置绑定类,在配置读取完成后赋值,便于使用
        scanResult.getClassesWithAnnotation(ConfigurationProperties.class.getName())
                .filter(classInfoFilter)
                .filter(classInfo -> !classInfo.isAbstract() && classInfo.hasAnnotation(Component.class.getName()))
                .forEach(classInfo -> {
                    Class<?> clazz = classInfo.loadClass();
                    Component component = AnnotationUtil.getSynthesizedAnnotation(clazz, Component.class);
                    String configurationName = StrUtil.isBlank(component.value()) ? null : component.value();
                    ConfigurationProperties configurationProperties = AnnotationUtil.getAnnotationAlias(clazz, ConfigurationProperties.class);
                    //配置前缀,用于查找对应配置
                    String prefix = configurationProperties.value();
                    if (StrUtil.isBlank(prefix)) {
                        throw new ConfigurationNotBindPrefixException(classInfo.getName());
                    }
                    Map<String, BeanClassDefinition> configPropertityClassMap = gameApplicationContext.getConfigPropertityClassMap();
                    if (configPropertityClassMap.containsKey(prefix)) {
                        BeanClassDefinition beanClassDefinition = configPropertityClassMap.get(prefix);
                        throw new ConfigurationPrefixRepeatException(prefix, clazz.getName(),
                                beanClassDefinition.getClazz().getName());
                    }
                    BeanClassDefinition beanClassDefinition = new BeanClassDefinition();
                    beanClassDefinition.setBeanName(configurationName);
                    beanClassDefinition.setClazz(clazz);
                    beanClassDefinition.setSingleton(true);
                    configPropertityClassMap.put(prefix, beanClassDefinition);
                });
    }

    /**
     * 处理属性配置值注入
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param scanResult             类扫描结果
     * @param classInfoFilter        类过滤器
     */
    private void handleFiledConfigInjectValue(GameApplicationContext gameApplicationContext, ScanResult scanResult,
                                              ClassInfoList.ClassInfoFilter classInfoFilter) {
        //查询所有的配置注入注解
        scanResult.getClassesWithFieldAnnotation(Value.class.getName())
                .filter(classInfoFilter)
                .filter(classInfo -> !classInfo.isAbstract())
                .filter(classInfo -> classInfo.hasAnnotation(Component.class.getName()))
                .forEach(classInfo -> {
                    Class<?> clazz = classInfo.loadClass();
                    classInfo.getFieldInfo()
                            .filter(fieldInfo -> fieldInfo.hasAnnotation(Value.class.getName()))
                            .forEach(fieldInfo -> {
                                AnnotationInfo annotationInfo = fieldInfo.getAnnotationInfo(Value.class.getName());
                                String eval = (String) annotationInfo.getParameterValues().getValue("value");
                                TypeSignature typeDescriptor = fieldInfo.getTypeDescriptor();
                                Class<?> fieldType;
                                if (typeDescriptor instanceof ClassRefTypeSignature classRefTypeSignature) {
                                    fieldType = classRefTypeSignature.loadClass();
                                } else {
                                    //基础类型
                                    BaseTypeSignature baseTypeSignature = (BaseTypeSignature) typeDescriptor;
                                    fieldType = baseTypeSignature.getType();
                                }
                                if (!isPrimitiveOrString(fieldType)) {
                                    //复杂的属性绑定使用配置绑定类
                                    log.warn("字段配置值注入-复杂属性注入请使用配置绑定类,当前类型：{} ,字段名：{} 已跳过",
                                            fieldType.getName(), fieldInfo.getName());
                                    return;
                                }
                                //获取filed
                                Field field = fieldInfo.loadClassAndGetField();

                                //处理表达式
                                Serializable expression = MvelUtils.compileExpression(eval);

                                ValueConfigBindBeanClassDefinition beanClassDefinition = new ValueConfigBindBeanClassDefinition();
                                beanClassDefinition.setClazz(clazz);
                                beanClassDefinition.setFiledClass(fieldType);

                                ReflectionUtils.makeAccessible(field);
                                beanClassDefinition.setField(field);
                                beanClassDefinition.setExpression(expression);

                                gameApplicationContext.getConfigBindKeyMap()
                                        .computeIfAbsent(clazz, k -> new ArrayList<>())
                                        .add(beanClassDefinition);
                            });
                });
    }

    /**
     * 处理初始化注解执行方法
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param scanResult             类扫描结果
     * @param classInfoFilter        类过滤器
     */
    private void handlePostConstructMethod(GameApplicationContext gameApplicationContext, ScanResult scanResult,
                                           ClassInfoList.ClassInfoFilter classInfoFilter) {
        //查询所有的配置注入注解
        scanResult.getClassesWithMethodAnnotation(PostConstruct.class.getName())
                .filter(classInfoFilter)
                .filter(classInfo -> !classInfo.isAbstract())
                .filter(classInfo -> classInfo.hasAnnotation(Component.class.getName()))
                .forEach(classInfo -> {
                    Class<?> clazz = classInfo.loadClass();
                    classInfo.getMethodInfo()
                            .filter(methodInfo -> methodInfo.hasAnnotation(PostConstruct.class.getName()))
                            //初始化方法必须是无参方法
                            .filter(methodInfo -> methodInfo.getParameterInfo().length == 0)
                            .forEach(methodInfo -> {
                                Method method = methodInfo.loadClassAndGetMethod();

                                BeanClassDefinition beanClassDefinition = new BeanClassDefinition();
                                beanClassDefinition.setClazz(clazz);

                                ReflectionUtils.makeAccessible(method);
                                beanClassDefinition.setMethod(method);

                                gameApplicationContext.getPostConstructMethodMap()
                                        .computeIfAbsent(clazz, k -> new ArrayList<>())
                                        .add(beanClassDefinition);
                            });
                });
    }

    /**
     * 处理配置类注入
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param scanResult             类扫描结果
     * @param classInfoFilter        类过滤器
     */
    private void handleConfigurationInjectClass(GameApplicationContext gameApplicationContext, ScanResult scanResult,
                                                ClassInfoList.ClassInfoFilter classInfoFilter) {
        //查找所有的配置类,在所有非动态依赖加载完成后加载配置类中的动态依赖
        scanResult.getClassesWithAnnotation(Configuration.class.getName())
                .filter(classInfoFilter)
                .filter(classInfo -> !classInfo.isAbstract())
                .forEach(classInfo -> {
                    Class<?> configurationClass = classInfo.loadClass();
                    Configuration configuration = configurationClass.getAnnotation(Configuration.class);
                    List<ConfigurationBeanClassDefinition> beanClassDefinitions = classInfo.getDeclaredMethodInfo()
                            //必须要是public的无参方法
                            .filter(methodInfo -> methodInfo.isPublic()
                                    && methodInfo.hasAnnotation(Bean.class.getName())
                                    && methodInfo.getParameterInfo().length == 0)
                            .stream()
                            .map(methodInfo -> {
                                Method method = methodInfo.loadClassAndGetMethod();
                                Bean bean = AnnotationUtil.getSynthesizedAnnotation(method, Bean.class);
                                String name = bean.name();
                                if (StrUtil.isNotBlank(bean.value())) {
                                    name = bean.value();
                                }
                                if (StrUtil.isBlank(name)) {
                                    name = null;
                                }
                                //获取返回值类型
                                ClassRefTypeSignature resultType = (ClassRefTypeSignature) methodInfo
                                        .getTypeDescriptor().getResultType();
                                Class<?> resultClass = resultType.loadClass();
                                //判断是否有原型类注解
                                boolean isSingleton = methodInfo.hasAnnotation(Prototype.class.getName());
                                ConfigurationBeanClassDefinition beanClassDefinition = new ConfigurationBeanClassDefinition();
                                beanClassDefinition.setConfigurationClass(configurationClass);
                                beanClassDefinition.setConfigurationBeanName(configuration.value());
                                beanClassDefinition.setBeanName(name);
                                beanClassDefinition.setSingleton(isSingleton);
                                beanClassDefinition.setClazz(resultClass);

                                ReflectionUtils.makeAccessible(method);
                                beanClassDefinition.setMethod(method);

                                //判断这个类有没有父类或者接口
                                ClassInfo resultClassInfo = getClassInfoByClass(scanResult, classInfoFilter, resultClass);
                                if (resultClassInfo != null) {
                                    boolean hasPrimary = methodInfo.hasAnnotation(Primary.class.getName());
                                    beanClassDefinition.setPrimary(hasPrimary);
                                    //处理继承关系
                                    handleExtendClassInfo(gameApplicationContext, resultClassInfo, name, beanClassDefinition);
                                    //处理接口关系
                                    String finalName = StringUtils.isBlank(name) ?
                                            StringUtils.toLowerCaseCamel(resultClassInfo.getSimpleName()) : name;
                                    resultClassInfo.getInterfaces()
                                            .forEach(interfaceClassInfo ->
                                                    checkInterfaceRepeatName(gameApplicationContext, interfaceClassInfo,
                                                            resultClass, finalName).put(finalName, beanClassDefinition));
                                }
                                return beanClassDefinition;
                            })
                            .toList();
                    gameApplicationContext.getConfigurationClassMap()
                            .put(configurationClass, beanClassDefinitions);
                });
    }

    /**
     * 根据类获取扫描的类信息
     *
     * @param scanResult      扫描结果
     * @param classInfoFilter 类过滤器
     * @param clazz           指定的类信息
     * @return
     */
    private ClassInfo getClassInfoByClass(ScanResult scanResult, ClassInfoList.ClassInfoFilter classInfoFilter, Class<?> clazz) {
        ClassInfoList classInfos = scanResult.getAllClasses()
                .filter(classInfoFilter)
                .filter(resultClassInfo -> !resultClassInfo.isAbstract())
                .filter(resultClassInfo -> !resultClassInfo.isInterface())
                .filter(resultClassInfo -> resultClassInfo.getName().equals(clazz.getName()));
        if (classInfos.isEmpty()) {
            //没有扫描到的类不做处理[需要使用的类一定会被扫描到]
            return null;
        }
        return classInfos.get(0);
    }

    /**
     * 处理固定注入的类
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param scanResult             类扫描结果
     * @param classInfoFilter        类过滤
     */
    @SuppressWarnings("unchecked")
    private void handleFixedInjectClass(GameApplicationContext gameApplicationContext, ScanResult scanResult,
                                        ClassInfoList.ClassInfoFilter classInfoFilter) {
        //找到所有需要进行依赖加载的类,默认都是单例的，只有@Bean注解配合@Prototype才会是非单例
        scanResult.getClassesWithAnnotation(Component.class.getName())
                .filter(classInfoFilter)
                .filter(classInfo -> !classInfo.isAnnotation() && !classInfo.isInterface()
                        && !classInfo.isAbstract() && !classInfo.isEnum()
                        && !classInfo.hasAnnotation(ConfigurationProperties.class.getName()))
                //直接全部注入，方便直接按实例注入
                .forEach(classInfo -> {
                    Class<?> clazz = classInfo.loadClass();
                    Component component = AnnotationUtil.getSynthesizedAnnotation(clazz, Component.class);
                    String beanName = component.value();
                    if (StrUtil.isBlank(beanName)) {
                        beanName = null;
                    }
                    boolean isSingleton = !classInfo.hasAnnotation(Prototype.class.getName());
                    BeanClassDefinition beanClassDefinition = new BeanClassDefinition();
                    beanClassDefinition.setClazz(clazz);
                    beanClassDefinition.setBeanName(beanName);
                    beanClassDefinition.setSingleton(isSingleton);
                    gameApplicationContext.getClassSet().add(beanClassDefinition);

                    //处理继承相关
                    boolean hasPrimary = classInfo.hasAnnotation(Primary.class.getName());
                    beanClassDefinition.setPrimary(hasPrimary);
                    handleExtendClassInfo(gameApplicationContext, classInfo, beanName, beanClassDefinition);
                });

        //获取所有的接口信息,判断对应的实现类是否进行依赖注入了，便于通过接口直接注入所有的实现类
        //和使用别名注入指定实现类
        //k:接口类型 v:实现类列表
        ClassInfoList allInterfaces = scanResult.getAllInterfaces()
                .filter(classInfoFilter)
                .filter(classInfo -> !classInfo.getName().equals(Provider.class.getName()));
        for (ClassInfo anInterface : allInterfaces) {
            ClassInfoList implementing = scanResult.getClassesImplementing(anInterface.getName())
                    .filter(classInfoFilter);
            for (ClassInfo classInfo : implementing) {
                boolean isSingleton = !classInfo.hasAnnotation(Prototype.class.getName());
                boolean b = classInfo.hasAnnotation(Component.class.getName());
                if (!b) {
                    continue;
                }
                Class<?> clazz = classInfo.loadClass();
                Component component = AnnotationUtil.getSynthesizedAnnotation(clazz, Component.class);
                String componentName = component.value();
                if (StrUtil.isBlank(componentName)) {
                    //没有名字就使用类名(首字母小写的驼峰格式)
                    componentName = StringUtils.toLowerCaseCamel(classInfo.getSimpleName());
                }
                Map<String, BeanClassDefinition> componetNameClassMap = checkInterfaceRepeatName(gameApplicationContext, anInterface, clazz, componentName);
                BeanClassDefinition beanClassDefinition = new BeanClassDefinition();
                beanClassDefinition.setClazz(clazz);
                beanClassDefinition.setBeanName(componentName);
                beanClassDefinition.setSingleton(isSingleton);
                componetNameClassMap.put(componentName, beanClassDefinition);
            }
        }

        //查找guice provider定义的依赖注入
        scanResult.getClassesImplementing(Provider.class.getName())
                .filter(classInfoFilter)
                .filter(classInfo -> !classInfo.isAbstract()
                        && classInfo.hasAnnotation(Component.class.getName()))
                .forEach(classInfo -> {
                    //加载类获取泛型
                    Class<? extends Provider<?>> providerClass = (Class<? extends Provider<?>>) classInfo.loadClass();
                    for (Type genericInterface : providerClass.getGenericInterfaces()) {
                        if (genericInterface instanceof ParameterizedType pt) {
                            if (!pt.getRawType().equals(Provider.class)) {
                                continue;
                            }
                            //Provider只会有一个泛型
                            Type actualTypeArgument = pt.getActualTypeArguments()[0];
                            if (actualTypeArgument instanceof Class<?> clazz) {
                                if (isPrimitiveOrString(clazz)) {
                                    log.warn("检查到有基础类型的provider,不会自动注入。请到module中指定使用！！！");
                                    continue;
                                }
                                Map<Class<?>, ProviderClassDefinition> providerClassMap =
                                        gameApplicationContext.getProviderClassMap();
                                if (providerClassMap.containsKey(clazz)) {
                                    ProviderClassDefinition providerClassDefinition = providerClassMap.get(clazz);
                                    throw new GuiceProviderRepeatException(clazz.getName(), providerClass.getName(),
                                            providerClassDefinition.getClazz().getName());
                                }
                                Component component = AnnotationUtil.getSynthesizedAnnotation(providerClass, Component.class);
                                String beanName = component.value();
                                if (StrUtil.isBlank(beanName)) {
                                    beanName = null;
                                }
                                boolean isSingleton = !classInfo.hasAnnotation(Prototype.class.getName());
                                ProviderClassDefinition providerClassDefinition = new ProviderClassDefinition();
                                providerClassDefinition.setBeanName(beanName);
                                providerClassDefinition.setClazz(providerClass);
                                providerClassDefinition.setSingleton(isSingleton);
                                providerClassMap.put(clazz, providerClassDefinition);
                            }
                        }
                    }
                });
    }

    /**
     * 检查接口名是否重复
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param interfaceClassInfo     接口定义信息
     * @param clazz                  实现类
     * @param beanNme                实现类名
     * @return
     */
    private Map<String, BeanClassDefinition> checkInterfaceRepeatName(GameApplicationContext gameApplicationContext,
                                                                      ClassInfo interfaceClassInfo,
                                                                      Class<?> clazz, String beanNme) {
        Map<String, BeanClassDefinition> componetNameClassMap = gameApplicationContext.getInterfaceClassMap()
                .computeIfAbsent(interfaceClassInfo.loadClass(), k -> new HashMap<>());
        if (componetNameClassMap.containsKey(beanNme)) {
            BeanClassDefinition beanClassDefinition = componetNameClassMap.get(beanNme);
            throw new ComponentNameRepeatException(beanNme, clazz.getName(),
                    beanClassDefinition.getClazz().getName());
        }
        return componetNameClassMap;
    }

    /**
     * 处理继承类相关信息
     *
     * @param gameApplicationContext 当前Verticle上下文
     * @param classInfo              需要处理的类信息
     * @param beanName               类名
     * @param beanClassDefinition    类定义
     */
    private void handleExtendClassInfo(GameApplicationContext gameApplicationContext, ClassInfo classInfo, String beanName, BeanClassDefinition beanClassDefinition) {
        Map<Class<?>, Map<String, BeanClassDefinition>> extendClassMap = gameApplicationContext.getExtendClassMap();
        String finalBeanName = StringUtils.isBlank(beanName) ?
                StringUtils.toLowerCaseCamel(classInfo.getSimpleName()) : beanName;
        classInfo.getSuperclasses()
                .forEach(superClassInfo -> {
                    Class<?> superClass = superClassInfo.loadClass();
                    extendClassMap.computeIfAbsent(superClass, k -> new HashMap<>())
                            .put(finalBeanName, beanClassDefinition);
                });
    }

    /**
     * 处理需要部署的verticle信息
     *
     * @param verticleClassInfoList vertilce的类信息
     */
    @SuppressWarnings("unchecked")
    private void handleDeployVerticle(ClassInfoList verticleClassInfoList) {
        //扫描所有要部署的verticle,并且优先级分组。
        verticleClassInfoList
                //排除掉引导的verticle
                .filter(classInfo -> !isBootstrapVerticleClassInfo(classInfo))
                .forEach(classInfo -> {
                    boolean isSingleton = !classInfo.hasAnnotation(Prototype.class.getName());
                    Class<? extends BaseVerticle> clazz = (Class<? extends BaseVerticle>) classInfo.loadClass();
                    DeployVerticle deployVerticle = clazz.getAnnotation(DeployVerticle.class);
                    int order = deployVerticle.order();
                    BaseVerticleClassDefinition beanClassDefinition = new BaseVerticleClassDefinition();
                    beanClassDefinition.setClazz(clazz);
                    beanClassDefinition.setBeanName(deployVerticle.value());
                    beanClassDefinition.setSingleton(isSingleton);
                    beanClassDefinition.setDeployConfigName(deployVerticle.deployConfigName());
                    beanClassDefinition.setEventSource(deployVerticle.eventSource());
                    GameApplicationContextFactory.getBootstrapApplicationContext()
                            .getVerticleClassTreeMap()
                            .computeIfAbsent(order, k -> new ArrayList<>())
                            .add(beanClassDefinition);
                });
    }

    /**
     * 扫描应用的类信息
     *
     * @return
     */
    private ScanResult scanApplicationClassInfos() {
        //获取启动类注解
        VertxGameApplication vertxGameApplication = AnnotationUtil.getAnnotation(startClass, VertxGameApplication.class);
        Class<?>[] exclude = vertxGameApplication.excludeClasses();
        String[] excludeClass = Arrays.stream(exclude)
                .map(Class::getName)
                .toArray(String[]::new);
        String[] excludePackageName = vertxGameApplication.excludePackageName();
        Class<?>[] scanBasePackageClasses = vertxGameApplication.scanBasePackageClasses();
        String[] scanBasePackageClassesName = Arrays.stream(scanBasePackageClasses)
                .map(Class::getName)
                .toArray(String[]::new);
        String[] scanBasePackages = vertxGameApplication.scanBasePackages();

        //获取额外需要管理的guice模块
        Class<Module>[] extraGuiceModules = vertxGameApplication.extraGuiceModule();
        registerGuiceModule(extraGuiceModules);


        //扫描需要的类
        return new ClassGraph()
                //允许所有信息
                .enableAllInfo()
                //黑名单
                .blacklistClasses(excludeClass)
                .blacklistPackages(excludePackageName)
                //白名单
                .whitelistPackages(startClass.getPackageName())
                .whitelistPackages(scanBasePackages)
                //固定扫描框架的包
                .whitelistPackages(BASE_SCAN_PACKAGE_PATH)
                .whitelistClasses(scanBasePackageClassesName)
                .scan();
    }

    /**
     * 注册guice的modules
     *
     * @param moduleClasses 模块类
     */
    private void registerGuiceModule(Class<Module>[] moduleClasses) {
        for (Class<Module> moduleClass : moduleClasses) {
            try {
                Module module = moduleClass.getConstructor().newInstance();
                this.extraModules.add(module);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new GuiceModuleInitException(moduleClass.getName(), e);
            }
        }
    }

    /**
     * 加载应用配置
     *
     * @param vertx                    vertx试炼
     * @param profile                  当前环境
     * @param customConfigStoreOptions 额外自定义配置
     * @return
     */
    private Future<JsonObject> loadApplicationProfileConfig(Vertx vertx, String profile, List<CustomConfigStoreOptions> customConfigStoreOptions) {
        if (DEFAULT_PROFILES_ACTIVE_STOP.equals(profile)) {
            return Future.succeededFuture(new JsonObject());
        }
        //依次读取classpath下的，和同目录下的
        //读取同目录下的
        String profileConfigPath = getProfileConfigPath(profile, DEFAULT_CONFIG_PATH);
        String priorityProfileConfigPath = getProfileConfigPath(profile, DEFAULT_CONFIG_PATH_PRIORITY);
        ConfigStoreOptions simpleStoreOptions = getConfigStoreOptionsByYaml(profileConfigPath + CONFIG_YAML_SUFFIX_SIMPLE);
        ConfigStoreOptions storeOptions = getConfigStoreOptionsByYaml(profileConfigPath + CONFIG_YAML_SUFFIX);
        ConfigStoreOptions simpleStoreOptionsPriority = getConfigStoreOptionsByYaml(priorityProfileConfigPath + CONFIG_YAML_SUFFIX_SIMPLE);
        ConfigStoreOptions storeOptionsPriority = getConfigStoreOptionsByYaml(priorityProfileConfigPath + CONFIG_YAML_SUFFIX);

        //读取工作目录下的
        String workDir = System.getProperty("user.dir");
        profileConfigPath = workDir + File.separator + profileConfigPath;
        priorityProfileConfigPath = workDir + File.separator + priorityProfileConfigPath;

        ConfigStoreOptions workDirSimpleStoreOptions = getConfigStoreOptionsByYaml(profileConfigPath + CONFIG_YAML_SUFFIX_SIMPLE);
        ConfigStoreOptions workDirStoreOptions = getConfigStoreOptionsByYaml(profileConfigPath + CONFIG_YAML_SUFFIX);
        ConfigStoreOptions workDirSimpleStoreOptionsPriority = getConfigStoreOptionsByYaml(priorityProfileConfigPath + CONFIG_YAML_SUFFIX_SIMPLE);
        ConfigStoreOptions workDirStoreOptionsPriority = getConfigStoreOptionsByYaml(priorityProfileConfigPath + CONFIG_YAML_SUFFIX);

        //按顺序读取 classpath <- 外部目录
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(simpleStoreOptions)
                .addStore(storeOptions)
                .addStore(simpleStoreOptionsPriority)
                .addStore(storeOptionsPriority)
                .addStore(workDirSimpleStoreOptions)
                .addStore(workDirStoreOptions)
                .addStore(workDirSimpleStoreOptionsPriority)
                .addStore(workDirStoreOptionsPriority);

        //可以添加自定义的配置中心,根据优先级进行覆盖
        if (customConfigStoreOptions != null) {
            customConfigStoreOptions.stream()
                    .filter(CustomConfigStoreOptions::isEnable)
                    .sorted(Comparator.comparingInt(CustomConfigStoreOptions::getOrder))
                    .forEach(configStoreOptions -> {
                        //设置当前配置环境
                        JsonObject config = configStoreOptions.getConfig();
                        if (config == null) {
                            config = new JsonObject();
                            configStoreOptions.setConfig(config);
                        }
                        config.put("profile", profile);
                        options.addStore(configStoreOptions);
                    });
        }


        //保存配置中心地址,会在引导verticle中监听配置改变
        GameApplicationContextFactory.getBootstrapApplicationContext().setConfigRetrieverOptions(options);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        // 获取配置
        return retriever.getConfig()
                .compose(configPriorityJson -> {
                    //关闭临时的配置读取
                    retriever.close();
                    return Future.succeededFuture(configPriorityJson);
                });
    }

    /**
     * 加载类路径下的应用配置
     *
     * @param yaml    yaml解析器
     * @param profile 当前环境
     * @return
     */
    private JsonObject loadClasspathAppConfig(Yaml yaml, String profile) {
        JsonObject appConfig = loadClasspathConfig(yaml,
                getProfileConfigPath(profile, DEFAULT_CONFIG_PATH));
        JsonObject appConfigPriority = loadClasspathConfig(yaml,
                getProfileConfigPath(profile, DEFAULT_CONFIG_PATH_PRIORITY));
        if (appConfig == null && appConfigPriority == null) {
            return null;
        }
        if (appConfig == null) {
            return appConfigPriority;
        } else if (appConfigPriority == null) {
            return appConfig;
        }
        return appConfig.mergeIn(appConfigPriority, true);
    }

    /**
     * 加载类路径下的配置
     *
     * @param yaml       yaml解析器
     * @param configPath 配置路径
     * @return
     */
    private JsonObject loadClasspathConfig(Yaml yaml, String configPath) {
        String applicationConfig;
        try {
            applicationConfig = ResourceUtil.readStr(configPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            //忽略没有对应的文件
            return null;
        }
        Object configObject = yaml.loadAs(applicationConfig, Object.class);
        return JsonObject.mapFrom(configObject);
    }

    /**
     * 添加vertx 的配置，会在部署配置中使用
     */
    @Override
    public void afterConfigParsed(JsonObject config) {
        if (!vertxConfig.isEmpty()) {
            //添加自定义的vertx配置
            vertxConfig.forEach(config::put);
        }
        super.afterConfigParsed(config);
    }

    /**
     * vertx 实例化的配置
     */
    @Override
    public void beforeStartingVertx(VertxOptions options) {
        BootstrapApplicationContext context = GameApplicationContextFactory.getBootstrapApplicationContext();
        VertxOptions vertxOptions = context.getApplicationConfig().getVertxOptions();
        BeanUtil.copyProperties(vertxOptions, options);
        if (updateVertxOptions != null) {
            updateVertxOptions.accept(options);
        }
        super.beforeStartingVertx(options);
    }

    /**
     * vertx实例化后，运行前修改
     */
    @Override
    public void afterStartingVertx(Vertx vertx) {
        if (customAfterStartingVertx != null) {
            customAfterStartingVertx.accept(vertx);
        }
        super.afterStartingVertx(vertx);
    }

    /**
     * Verticle 部署前修改，可以修改部署配置，高可用，vertx实例...
     */
    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
        BootstrapApplicationContext context = GameApplicationContextFactory.getBootstrapApplicationContext();
        DeploymentOptions verticleDeploymentOptions = context.getApplicationConfig().getDeploymentOptions();
        BeanUtil.copyProperties(verticleDeploymentOptions, deploymentOptions);
        if (updateBeforeDeployingVerticleOptions != null) {
            updateBeforeDeployingVerticleOptions.accept(deploymentOptions);
        }
        super.beforeDeployingVerticle(deploymentOptions);
        //引导类固定只有一个实例
        deploymentOptions.setInstances(1);
    }

    /**
     * vertx 停止前处理
     */
    @Override
    public void beforeStoppingVertx(Vertx vertx) {
        if (updateBeforeStoppingVertx != null) {
            updateBeforeStoppingVertx.accept(vertx);
        }
        super.beforeStoppingVertx(vertx);
    }

    /**
     * 停止后处理
     */
    @Override
    public void afterStoppingVertx() {
        if (vertxAfterStopHandle != null) {
            vertxAfterStopHandle.handle();
        }
        super.afterStoppingVertx();
    }

    /**
     * vertx 部署失败处理
     */
    @Override
    public void handleDeployFailed(Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause) {
        if (verticleDeployFailedHandle != null) {
            verticleDeployFailedHandle.handle(vertx, mainVerticle, deploymentOptions, cause);
        }
        super.handleDeployFailed(vertx, mainVerticle, deploymentOptions, cause);
    }

    /**
     * 添加vertx启动配置
     *
     * @param key
     * @param vale
     */
    public void addVertxConfig(String key, Object vale) {
        this.vertxConfig.put(key, vale);
    }

    /**
     * 添加vertx启动配置
     *
     * @param configMap
     */
    public void addAllVertxConfig(Map<String, Object> configMap) {
        this.vertxConfig.putAll(configMap);
    }

    /**
     * 获取指定环境对应的配置路径
     *
     * @param profile        环境
     * @param baseConfigPath 配置基础路径
     * @return
     */
    protected String getProfileConfigPath(String profile, String baseConfigPath) {
        if (StrUtil.isBlank(profile)) {
            return baseConfigPath;
        }
        return baseConfigPath + "-" + profile;
    }

    /**
     * 根据路径获取yml配置文件配置中心地址
     *
     * @param path
     * @return
     */
    protected ConfigStoreOptions getConfigStoreOptionsByYaml(String path) {
        return new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setOptional(true)
                .setConfig(new JsonObject()
                        .put("path", path)
                );
    }

    /**
     * 设置json参数
     */
    protected void configureJson() {
        //获取vertx的json处理对象
        ObjectMapper objectMapper = DatabindCodec.mapper();
        SimpleModule module = new SimpleModule();
        //执行自定义处理
        if (customGameJacksonConfig != null) {
            customGameJacksonConfig.config(objectMapper, module);
        }

        //默认处理
        var pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        //时间序列化格式化
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(pattern));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(pattern));
        //适配vertx的json
        module.addDeserializer(JsonObject.class, new JsonObjectDeserializer());
        module.addDeserializer(JsonArray.class, new JsonArrayDeserializer());

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new VertxModule());
        objectMapper.registerModule(module);
        //非null属性才序列化
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //枚举使用索引代替
        objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true);
        //允许序列化是有未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

}
