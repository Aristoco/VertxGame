package com.aristoco.core.event;

import cn.hutool.core.lang.Pair;
import com.aristoco.core.GameApplicationContext;
import com.aristoco.core.annotation.Component;
import com.aristoco.core.annotation.Prototype;
import com.aristoco.core.bean.definition.EventListenerBeanClassDefinition;
import com.aristoco.core.config.ApplicationEventExecutorConfig;
import com.aristoco.core.exception.MvelCompileExpressionException;
import com.aristoco.core.utils.ClassUtils;
import com.aristoco.core.utils.MvelUtils;
import com.aristoco.core.utils.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author chenguowei
 * @date 2024/7/8
 * @description vertx的eventbus事件多播管理器
 **/
@Slf4j
@Component
@Prototype
public class EventBusApplicationEventMulticaster implements ApplicationEventMulticaster {

    /**
     * eventbus上事件传递前缀
     * 防止重复,使用类名加指定的信息
     */
    private final String EVENT_BUS_PREFIX = ClassUtils.getClassLowerCaseCamel(this.getClass()) +
            ".eventbus.event.";

    /**
     * eventbus上包装类传递的事件前缀
     */
    private final String EVENT_BUS_PAYLOAD_PREFIX = EVENT_BUS_PREFIX + "payload.";

    /**
     * 应用上下文
     */
    private final GameApplicationContext context;

    /**
     * vertx
     */
    private final Vertx vertx;

    /**
     * vertx的eventbus
     */
    private final EventBus eventBus;

    /**
     * 事件线程池配置
     */
    private final ApplicationEventExecutorConfig applicationEventExecutorConfig;

    /**
     * eventbus的发送配置
     */
    private final DeliveryOptions eventbusDeliveryOptions;

    /**
     * 共享事件监听器列表
     * k:事件地址  v:[{事件类,事件执行器},...]
     */
    private final Map<String, List<Pair<Class<?>, EventListenHandler<? extends ApplicationEvent>>>> shareEventListenerMap = new HashMap<>();

    /**
     * 独立事件监听器列表
     * k:事件地址  v:[{事件类,事件执行器},...]
     */
    private final Map<String, List<Pair<Class<?>, EventListenHandler<? extends ApplicationEvent>>>> aloneEventListenerMap = new HashMap<>();

    /**
     * 共享事件监听器列表
     * k:事件地址  v:[{事件类,事件执行器},...]
     */
    private final Map<String, List<Pair<Class<?>, EventListenHandler<? extends ApplicationEvent>>>> localShareEventListenerMap = new HashMap<>();

    /**
     * 独立事件监听器列表
     * k:事件地址  v:[{事件类,事件执行器},...]
     */
    private final Map<String, List<Pair<Class<?>, EventListenHandler<? extends ApplicationEvent>>>> localAloneEventListenerMap = new HashMap<>();

    @Inject
    public EventBusApplicationEventMulticaster(GameApplicationContext context, Vertx vertx, EventBus eventBus,
                                               ApplicationEventExecutorConfig applicationEventExecutorConfig) {
        this.context = context;
        this.vertx = vertx;
        this.eventBus = eventBus;
        this.applicationEventExecutorConfig = applicationEventExecutorConfig;
        this.eventbusDeliveryOptions = applicationEventExecutorConfig.getEventOptions().toDeliveryOptions();
    }

    /**
     * 执行初始化
     */
    @PostConstruct
    public void init() {
        //初始化事件监听器
        initEventListener();

        //注册事件监听器到eventbus上
        registerListenerToEventBus();

    }

    /**
     * 注册事件监听器到eventbus
     */
    private void registerListenerToEventBus() {
        WorkerExecutor eventBusEventExecutor;
        ApplicationEventExecutorConfig.ExecutorConfig executorConfig =
                applicationEventExecutorConfig.getExecutor();
        if (executorConfig.isEnable()) {
            //创建一个事件专用的命名工作work-pool
            String name = executorConfig.getName();
            int poolSize = executorConfig.getPoolSize();
            long maxExecuteTime = executorConfig.getMaxExecuteTime();
            TimeUnit maxExecuteTimeUnit = executorConfig.getMaxExecuteTimeUnit();
            eventBusEventExecutor = vertx.createSharedWorkerExecutor(name, poolSize, maxExecuteTime, maxExecuteTimeUnit);
        } else {
            eventBusEventExecutor = null;
        }
        //共享事件监听,一个地址转发多个监听
        registerShareEventListeners(eventBusEventExecutor, false);
        registerShareEventListeners(eventBusEventExecutor, true);
        //独立事件监听,每个监听各自独立
        registerAloneEventListeners(eventBusEventExecutor, false);
        registerAloneEventListeners(eventBusEventExecutor, true);
    }

    /**
     * 注册独立监听时间
     *
     * @param eventBusEventExecutor 事件执行线程池
     * @param isLocal               是否监听本地事件
     */
    @SuppressWarnings({"rawtypes"})
    private void registerAloneEventListeners(WorkerExecutor eventBusEventExecutor, boolean isLocal) {
        getEventListenerMap(true, isLocal)
                .forEach((listenAddr, listeners) -> {
                    //判断是否是payload监听地址
                    boolean isPayLoadEvent = listenAddr.startsWith(EVENT_BUS_PAYLOAD_PREFIX);
                    listeners.forEach(pair -> {
                        Class<?> eventClass = pair.getKey();
                        EventListenHandler listener = pair.getValue();
                        //事件处理在work-pool中，避免阻塞
                        MessageConsumer<Object> consumer = isLocal ?
                                eventBus.localConsumer(listenAddr) : eventBus.consumer(listenAddr);
                        consumer.handler(message -> {
                            JsonObject body = (JsonObject) message.body();
                            Callable<Object> callable = () -> {
                                //执行事件监听器
                                handleEventListener(listenAddr, isPayLoadEvent, eventClass, listener, body);
                                return null;
                            };
                            if (eventBusEventExecutor != null) {
                                //事件处理在work-pool中，避免阻塞
                                eventBusEventExecutor.executeBlocking(callable, applicationEventExecutorConfig
                                        .getExecutor().isOrder());
                            } else {
                                try {
                                    callable.call();
                                } catch (Exception e) {
                                    log.error("事件执行失败", e);
                                }
                            }
                        });
                    });
                });
    }

    /**
     * 注册共享事件监听
     *
     * @param eventBusEventExecutor 事件线程池
     * @param isLocal               是否本地监听
     */
    @SuppressWarnings({"rawtypes"})
    private void registerShareEventListeners(WorkerExecutor eventBusEventExecutor, boolean isLocal) {
        getEventListenerMap(false, isLocal)
                .forEach((listenAddr, listeners) -> {
                            //判断是否是payload监听地址
                            boolean isPayLoadEvent = listenAddr.startsWith(EVENT_BUS_PAYLOAD_PREFIX);
                            MessageConsumer<Object> consumer = isLocal ?
                                    eventBus.localConsumer(listenAddr) : eventBus.consumer(listenAddr);
                            consumer.handler(message -> {
                                JsonObject body = (JsonObject) message.body();
                                Callable<Object> callable = () -> {
                                    listeners.forEach(pair -> {
                                        Class<?> eventClass = pair.getKey();
                                        EventListenHandler listener = pair.getValue();
                                        //执行事件监听器
                                        handleEventListener(listenAddr, isPayLoadEvent, eventClass, listener, body);
                                    });
                                    return null;
                                };
                                if (eventBusEventExecutor != null) {
                                    //事件处理在work-pool中，避免阻塞
                                    eventBusEventExecutor.executeBlocking(callable,
                                            applicationEventExecutorConfig.getExecutor().isOrder());
                                } else {
                                    try {
                                        callable.call();
                                    } catch (Exception e) {
                                        log.error("事件执行失败", e);
                                    }
                                }
                            });
                        }
                );
    }

    /**
     * 执行监听器
     *
     * @param listenAddr     监听地址
     * @param isPayLoadEvent 是否是payload事件
     * @param eventClass     事件类型
     * @param listener       监听器
     * @param body           消息体
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handleEventListener(String listenAddr, boolean isPayLoadEvent, Class<?> eventClass,
                                     EventListenHandler listener, JsonObject body) {
        //包装一下,避免阻塞
        Future.succeededFuture()
                .compose(ignore -> {
                    try {
                        if (isPayLoadEvent) {
                            //判断监听的事件是否是payload事件
                            listener.accept(body.mapTo(PayloadApplicationEvent.class));
                        } else {
                            listener.accept(body.mapTo(eventClass));
                        }
                    } catch (Throwable e) {
                        log.error("事件执行失败,事件类型：{},事件消息:{}", listenAddr, body, e);
                    }
                    return Future.succeededFuture();
                });
    }

    /**
     * 初始化事件监听器
     */
    private void initEventListener() {
        List<EventListenerBeanClassDefinition> eventListeners = context.getEventListeners();
        eventListeners.forEach(beanClassDefinition -> {
            //处理当前监听的地址信息
            Set<Class<?>> listenerClasses = beanClassDefinition.getListenerClasses();
            listenerClasses.forEach(listenerClass -> {
                String listenAddr;
                if (ApplicationEvent.class.isAssignableFrom(listenerClass)) {
                    listenAddr = EVENT_BUS_PREFIX + listenerClass.getName();
                } else {
                    listenAddr = EVENT_BUS_PAYLOAD_PREFIX + listenerClass.getName();
                }
                //判断当前方法的类型
                Class<?> clazz = beanClassDefinition.getClazz();
                String beanName = beanClassDefinition.getBeanName();

                //获取对象
                Object bean = context.getBean(beanName, clazz);

                //判断是接口还是注解事件监听
                handleEventListerByType(beanClassDefinition, listenerClass, bean, listenAddr);
            });
        });
    }

    /**
     * 获取监听器缓存列表
     *
     * @param alone 是否独立
     * @param local 是否本地
     * @return
     */
    private Map<String, List<Pair<Class<?>, EventListenHandler<? extends ApplicationEvent>>>> getEventListenerMap(Boolean alone, Boolean local) {
        if (alone) {
            return local ? this.localAloneEventListenerMap : this.aloneEventListenerMap;
        } else {
            return local ? this.localShareEventListenerMap : this.shareEventListenerMap;
        }
    }

    /**
     * 根据类型处理事件监听器
     *
     * @param beanClassDefinition 监听器的类定义
     * @param listenerClass       监听的类型
     * @param bean                监听实例
     * @param listenAddr          监听地址
     */
    private void handleEventListerByType(EventListenerBeanClassDefinition beanClassDefinition, Class<?> listenerClass,
                                         Object bean, String listenAddr) {
        boolean isInterface = handleEventListerInterface(beanClassDefinition, listenerClass, bean, listenAddr);
        if (isInterface) {
            //是接口则接口处理
            return;
        }
        //处理监听注解
        List<EventListenerBeanClassDefinition.ParameterBeanClassDefinition> parameterBeanClassDefinitions =
                beanClassDefinition.getParameterBeanClassDefinitions();
        String condition = beanClassDefinition.getCondition();
        Serializable expression = compileExpression(condition, beanClassDefinition.getClazz().getName());
        Set<Class<?>> payloadClassSet = parameterBeanClassDefinitions.
                stream()
                .filter(EventListenerBeanClassDefinition.ParameterBeanClassDefinition::isPayloadEvent)
                .map(EventListenerBeanClassDefinition.ParameterBeanClassDefinition::getParameterClass)
                .collect(Collectors.toSet());
        getEventListenerMap(beanClassDefinition.isAlone(), beanClassDefinition.isLocal())
                .computeIfAbsent(listenAddr, k -> new ArrayList<>())
                .add(new Pair<>(payloadClassSet.contains(listenerClass) ? PayloadApplicationEvent.class : listenerClass,
                        event -> handleEventListenerByAnnotation(beanClassDefinition, listenerClass, bean, event, expression))
                );
    }

    /**
     * 执行注解的事件监听方法
     *
     * @param beanClassDefinition 事件监听方法的定义
     * @param listenerClass       监听的类
     * @param bean                监听的实例
     * @param event               监听的事件对象
     * @param expression          mvel表达式
     */
    @SuppressWarnings("rawtypes")
    private void handleEventListenerByAnnotation(EventListenerBeanClassDefinition beanClassDefinition,
                                                 Class<?> listenerClass, Object bean,
                                                 ApplicationEvent event, Serializable expression) throws Exception {
        Set<String> eventSources = beanClassDefinition.getEventSources();
        List<EventListenerBeanClassDefinition.ParameterBeanClassDefinition> parameterBeanClassDefinitions =
                beanClassDefinition.getParameterBeanClassDefinitions();
        boolean trigger = true;
        if (!eventSources.isEmpty()) {
            trigger = eventSources.contains(event.getSource());
        }
        if (!trigger) {
            return;
        }

        Method method = beanClassDefinition.getMethod();

        //判断是否执行
        Set<Class<?>> listenerClasses = beanClassDefinition.getListenerClasses();
        Map<String, Object> paramMap = new HashMap<>(listenerClasses.size());
        if (parameterBeanClassDefinitions.isEmpty()) {
            //无参数的监听，判断是否有条件
            if (expression != null) {
                listenerClasses.forEach(listenerClazz -> {
                    if (listenerClazz.isAssignableFrom(listenerClass)) {
                        paramMap.put(ClassUtils.getClassLowerCaseCamel(listenerClazz), event);
                    } else {
                        paramMap.put(ClassUtils.getClassLowerCaseCamel(listenerClazz), null);
                    }
                });
                trigger = MvelUtils.handleExpression(expression, paramMap, Boolean.class);
            }
            if (trigger) {
                method.invoke(bean);
            }
            return;
        }
        //构建参数列表
        Object[] args = parameterBeanClassDefinitions.stream()
                .map(definitions -> {
                    if (definitions.getParameterClass().isAssignableFrom(listenerClass)) {
                        //判断是否是事件类，不是事件类需要获取payload
                        if (ApplicationEvent.class.isAssignableFrom(listenerClass)) {
                            paramMap.put(definitions.getParameterName(), event);
                            return event;
                        } else {
                            //是payload包装过的
                            PayloadApplicationEvent payloadApplicationEvent =
                                    (PayloadApplicationEvent) event;
                            Object payload = payloadApplicationEvent.getPayload();
                            paramMap.put(definitions.getParameterName(), payload);
                            return payload;
                        }
                    }
                    paramMap.put(definitions.getParameterName(), null);
                    return null;
                })
                .toArray(Object[]::new);
        if (expression != null) {
            trigger = MvelUtils.handleExpression(expression, paramMap, Boolean.class);
        }
        if (trigger) {
            method.invoke(bean, args);
        }
    }

    /**
     * 处理监听器接口实现类
     *
     * @param beanClassDefinition 接口实现类定义
     * @param listenerClass       监听的类型
     * @param bean                监听的实例
     * @param listenAddr          监听的地址
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean handleEventListerInterface(EventListenerBeanClassDefinition beanClassDefinition,
                                               Class<?> listenerClass, Object bean,
                                               String listenAddr) {
        if (beanClassDefinition.getParameterBeanClassDefinitions() != null) {
            return false;
        }
        ApplicationListener listener = (ApplicationListener) bean;
        //处理监听器接口
        boolean payloadEvent = beanClassDefinition.isPayloadEvent();
        String parameterName = beanClassDefinition.getParameterName();
        //编译表达式
        String condition = listener.condition();
        Serializable expression = compileExpression(condition, bean.getClass().getName());

        //获取要监听的事件源
        Set<String> evenSources = Stream.of(listener.eventSources())
                .map(Class::getName)
                .collect(Collectors.toSet());

        getEventListenerMap(listener.isAlone(), listener.isLocal())
                .computeIfAbsent(listenAddr, k -> new ArrayList<>())
                //如果是payload包装事件传入payload
                .add(new Pair<>(payloadEvent ? PayloadApplicationEvent.class : listenerClass, event -> {
                            boolean trigger = true;
                            if (expression != null) {
                                trigger = MvelUtils.handleExpression(expression, Map.of(parameterName, event),
                                        Boolean.class);
                            }
                            if (!evenSources.isEmpty()) {
                                trigger = evenSources.contains(event.getSource());
                            }
                            if (trigger) {
                                listener.onApplicationEvent(event);
                            }
                        })
                );
        return true;
    }

    /**
     * 编译表达式
     *
     * @param expressionStr 表达式字符串
     * @param listenerName  监听器名字
     * @return
     */
    private Serializable compileExpression(String expressionStr, String listenerName) {
        if (StringUtils.isNotBlank(expressionStr)) {
            try {
                //处理为通用解析的模式,条件表达式不存在默认值情况
                if (expressionStr.startsWith("#{") || expressionStr.startsWith("${")) {
                    return MvelUtils.compileExpression(expressionStr);
                }
                return MvelUtils.compileExpression("#{" + expressionStr + "}");
            } catch (Throwable e) {
                throw new MvelCompileExpressionException(listenerName, expressionStr, e);
            }
        }
        return null;
    }

    /**
     * 将给定的事件发送到eventbus,事件名就是类名
     *
     * @param event  事件
     * @param direct 是否点对点【点对点模式将会只有一个消费者接收到】
     */
    @Override
    public void multicastEvent(ApplicationEvent event, boolean direct) {
        if (event instanceof PayloadApplicationEvent<?> e) {
            //直接发布事件对象,会经过包装,便于传输
            publishToEventBus(EVENT_BUS_PAYLOAD_PREFIX + e.getPayload().getClass().getName(),
                    JsonObject.mapFrom(e), direct);
        } else {
            publishToEventBus(EVENT_BUS_PREFIX + event.getClass().getName(),
                    JsonObject.mapFrom(event), direct);
        }
    }

    /**
     * 发布信息到eventbus上
     *
     * @param addr    发布地址
     * @param message 消息
     * @param direct  是否点对点【点对点模式将会只有一个消费者接收到】
     */
    private void publishToEventBus(String addr, JsonObject message, boolean direct) {
        if (direct) {
            eventBus.send(addr, message, eventbusDeliveryOptions);
            return;
        }
        eventBus.publish(addr, message, eventbusDeliveryOptions);
    }
}
