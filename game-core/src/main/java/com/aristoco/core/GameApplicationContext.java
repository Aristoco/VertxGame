package com.aristoco.core;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.lang.Assert;
import com.aristoco.core.bean.definition.*;
import com.aristoco.core.event.*;
import com.aristoco.core.exception.BeanNotFoundException;
import com.aristoco.core.exception.GameApplicationContextRefreshException;
import com.aristoco.core.utils.ApplicationEventUtils;
import com.aristoco.core.utils.ObjectUtils;
import com.aristoco.core.utils.StopWatchUtils;
import com.aristoco.core.utils.StringUtils;
import com.aristoco.core.vertx.verticle.BaseVerticle;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.util.*;

/**
 * @author chenguowei
 * @date 2024/6/14
 * @description 应用上下文
 **/
@Slf4j
public class GameApplicationContext implements ApplicationEventPublisher, ApplicationContext {


    /**
     * 事件多播器默认名
     * 判断是否注入了事件多播器，未注入则使用默认的
     *
     * @see EventBusApplicationEventMulticaster
     */
    public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

    /**
     * 启动时间
     */
    private long startupDate;

    /**
     * 当前上下文名
     */
    private final String displayName = ObjectUtils.identityToString(this);

    /**
     * 服务器启动秒表id
     */
    @Getter
    @Setter
    private String stopWatchId;

    /**
     * 上下文是否初始化完成
     */
    @Getter
    @Setter
    private boolean init = false;

    /**
     * 保存一下vertx便于上下文信息传递
     */
    @Getter
    @Setter
    private Vertx vertx;

    /**
     * 保存当前的依赖管理器
     */
    @Getter
    @Setter
    private Injector injector;

    /**
     * 保存当前所有的类
     */
    @Getter
    private final Set<BeanClassDefinition> classSet = new HashSet<>();

    /**
     * 保存所有接口和实现类的注入信息
     * k:接口类  v:k1:实现类名 v1:实现类
     */
    @Getter
    private final Map<Class<?>, Map<String, BeanClassDefinition>> interfaceClassMap = new HashMap<>();

    /**
     * 保存所有父类和子类类的注入信息
     * k:父类  v:k1:子类名 v1:子类类
     */
    @Getter
    private final Map<Class<?>, Map<String, BeanClassDefinition>> extendClassMap = new HashMap<>();

    /**
     * 保存所有通过provider构建的
     * k:依赖的类 v:provider接口实现类
     */
    @Getter
    private final Map<Class<?>, ProviderClassDefinition> providerClassMap = new HashMap<>();

    /**
     * 需要绑定配置的属性
     * k:需要配置类注入的类  v:要注入的类的属性定义信息
     */
    @Getter
    private final Map<Class<?>, List<ValueConfigBindBeanClassDefinition>> configBindKeyMap = new HashMap<>();

    /**
     * 需要初始化执行的类的定义
     * k:需要初始化执行的类  v:始化执行的类的定义信息
     */
    @Getter
    private final Map<Class<?>, List<BeanClassDefinition>> postConstructMethodMap = new HashMap<>();

    /**
     * 配置类
     * 在guice中通过实例绑定配置类中通过@Bean注解的方法，还要注意是否有@Prototype注解
     * k:配置类 v:配置类注入类型定义集合
     */
    @Getter
    private final Map<Class<?>, List<ConfigurationBeanClassDefinition>> configurationClassMap = new HashMap<>();

    /**
     * 配置绑定类
     * k:prefix 配置前缀  v:配置绑定类定义信息
     */
    @Getter
    private final Map<String, BeanClassDefinition> configPropertityClassMap = new HashMap<>();

    /**
     * 额外的guice依赖管理模块
     */
    @Getter
    private final List<Module> extraGuiceModule = new ArrayList<>();

    /**
     * 事件多播器
     */
    @Nullable
    private ApplicationEventMulticaster applicationEventMulticaster;

    /**
     * 事件多播器类定义
     */
    @Getter
    private final ApplicationEventMulticasterClassDefinition applicationEventMulticasterDefinition = new ApplicationEventMulticasterClassDefinition();

    /**
     * 事件多播器还未启动时的事件列表
     */
    @Nullable
    private Set<ApplicationEvent> earlyApplicationEvents;

    /**
     * 事件监听器
     */
    @Getter
    private final List<EventListenerBeanClassDefinition> eventListeners = new ArrayList<>();


    /**
     * 发布事件
     *
     * @param event  事件
     * @param direct 是否点对点【点对点模式将会只有一个消费者接收到】
     */
    @Override
    public void publishEvent(Object event, boolean direct) {
        Assert.notNull(event, "Event must not be null");

        // Decorate event as an ApplicationEvent if necessary
        ApplicationEvent applicationEvent;
        if (event instanceof ApplicationEvent appEvent) {
            applicationEvent = appEvent;
        } else {
            applicationEvent = new PayloadApplicationEvent<>(event);
        }

        //设置事件源,支持自定义源
        if (StringUtils.isBlank(applicationEvent.getSource())) {
            applicationEvent.setSource(ApplicationEventUtils.getCurrentEventSource());
        }

        if (this.earlyApplicationEvents != null) {
            this.earlyApplicationEvents.add(applicationEvent);
        } else {
            // 发送事件
            getApplicationEventMulticaster().multicastEvent(applicationEvent, direct);
        }
    }

    /**
     * 获取事件多播器
     *
     * @return
     * @throws IllegalStateException
     */
    ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
        if (this.applicationEventMulticaster == null) {
            throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
                    "call 'refresh' before multicasting events via the context: " + this);
        }
        return this.applicationEventMulticaster;
    }

    /**
     * 初始化事件多播器
     */
    protected void initApplicationEventMulticaster() {
        this.applicationEventMulticaster = getBean(GameApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
                ApplicationEventMulticaster.class);
        if (log.isTraceEnabled()) {
            log.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
        }
    }

    /**
     * 准备刷新上下文
     */
    public void prepareRefresh() {
        //记录启动时间
        this.startupDate = System.currentTimeMillis();

        //启动秒表计数
        startRecordTime();

        if (log.isDebugEnabled()) {
            if (log.isTraceEnabled()) {
                log.trace("Refreshing " + this);
            } else {
                log.debug("Refreshing " + getDisplayName());
            }
        }

        //初始化早期事件记录
        this.earlyApplicationEvents = new LinkedHashSet<>();
    }

    /**
     * 刷新上下文
     */
    public void onRefresh() {
        try {
            //初始化事件管理器
            initApplicationEventMulticaster();
        } catch (Throwable e) {
            throw new GameApplicationContextRefreshException(e);
        }
    }

    /**
     * 完成上下文刷新
     */
    public void finishRefresh() {
        try {
            //获取早期事件进行发布
            Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
            this.earlyApplicationEvents = null;
            if (!CollectionUtil.isEmpty(earlyEventsToProcess)) {
                for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
                    getApplicationEventMulticaster().multicastEvent(earlyEvent);
                }
            }

            // 发布事件
            publishEvent(new ContextRefreshedEvent());

            //启动完成时间打印
            stopRecordTime();
        } catch (Throwable e) {
            throw new GameApplicationContextRefreshException(e);
        }
    }

    /**
     * 获取启动时间
     *
     * @return
     */
    @Override
    public long getStartupDate() {
        return this.startupDate;
    }

    /**
     * 获取上下文名
     *
     * @return
     */
    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * 获取对应的bean
     *
     * @param tClass bean的类型
     * @param <T>
     * @return
     */
    public <T> T getBean(Class<T> tClass) {
        return getInjector().getInstance(tClass);
    }

    /**
     * 根据名字获取对应的bean
     *
     * @param beanName bean的名字
     * @param tClass   bean的类型
     * @param <T>
     * @return
     */
    public <T> T getBean(String beanName, Class<T> tClass) {
        if (StringUtils.isBlank(beanName)) {
            return getBean(tClass);
        }
        Binding<T> binding = getInjector()
                .getExistingBinding(Key.get(tClass,
                        Names.named(beanName)));
        if (binding == null) {
            throw new BeanNotFoundException(beanName, tClass.getName());
        }
        return binding.getProvider().get();
    }

    /**
     * 开始记录服务启动时间
     */
    public void startRecordTime() {
        StopWatch stopWatch = StopWatchUtils.createStopWatch();
        setStopWatchId(stopWatch.getId());
        String verticleName = Vertx.currentContext().get(BaseVerticle.VERTICLE_CLASS_NAME);
        stopWatch.start(verticleName + "服务加载启动");
    }

    /**
     * 停止记录启动时间
     */
    public void stopRecordTime() {
        StopWatch stopWatch = StopWatchUtils.getStopWatch(getStopWatchId());
        stopWatch.stop();
        double totalTimeSeconds = stopWatch.getTotalTimeSeconds();
        DecimalFormat df = new DecimalFormat("#0.000");
        String verticleName = Vertx.currentContext().get(BaseVerticle.VERTICLE_CLASS_NAME);
        log.info("{} 服务启动时间: {} s", verticleName, df.format(totalTimeSeconds));
        StopWatchUtils.removeStopWatch(getStopWatchId());
    }

    /**
     * 复制一个可用的上下文对象
     * <p> 注意不会复制共享对象
     * <p> beanDefinition有部分涉及到反射无法工具类复制
     *
     * @return 返回复制的对象
     */
    public GameApplicationContext copy() {
        GameApplicationContext context = new GameApplicationContext();
        //保存一下初始化结果
        context.setInit(isInit());

        //复制beanDefinition
        context.getClassSet().addAll(getClassSet());
        context.getInterfaceClassMap().putAll(getInterfaceClassMap());
        context.getExtendClassMap().putAll(getExtendClassMap());
        context.getProviderClassMap().putAll(getProviderClassMap());
        context.getConfigBindKeyMap().putAll(getConfigBindKeyMap());
        context.getPostConstructMethodMap().putAll(getPostConstructMethodMap());
        context.getConfigurationClassMap().putAll(getConfigurationClassMap());
        context.getConfigPropertityClassMap().putAll(getConfigPropertityClassMap());
        context.getExtraGuiceModule().addAll(getExtraGuiceModule());
        ApplicationEventMulticasterClassDefinition definition = context.getApplicationEventMulticasterDefinition();
        definition.setClazz(getApplicationEventMulticasterDefinition().getClazz());
        definition.setBeanName(getApplicationEventMulticasterDefinition().getBeanName());
        definition.setSingleton(getApplicationEventMulticasterDefinition().isSingleton());
        context.getEventListeners().addAll(getEventListeners());
        return context;
    }


}
