package com.aristoco.core;

import cn.hutool.core.bean.BeanUtil;
import com.aristoco.core.annotation.DeployVerticle;
import com.aristoco.core.bean.CommonEventSource;
import com.aristoco.core.bean.VerticleInfo;
import com.aristoco.core.bean.definition.BaseVerticleClassDefinition;
import com.aristoco.core.config.ApplicationConfig;
import com.aristoco.core.config.VerticleBaseConfig;
import com.aristoco.core.event.ApplicationConfigUpdateEvent;
import com.aristoco.core.event.BootstrapVerticleStopEvent;
import com.aristoco.core.event.VerticleStopEvent;
import com.aristoco.core.event.annotation.EventListener;
import com.aristoco.core.utils.ApplicationEventUtils;
import com.aristoco.core.utils.TimeUtils;
import com.aristoco.core.vertx.factory.DeployVerticleFactory;
import com.aristoco.core.vertx.verticle.BaseVerticle;
import com.google.inject.Inject;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author chenguowei
 * @date 2024/6/17
 * @description 引导verticle
 **/
@Slf4j
@DeployVerticle
public final class BootstrapVerticle extends BaseVerticle {

    @Inject
    private DeployVerticleFactory deployVerticleFactory;

    /**
     * 注入部署配置
     * k:部署配置名 v:具体的部署配置
     */
    @Inject
    private Map<String, VerticleBaseConfig> verticleBaseConfigMap;

    /**
     * bootstrap的上下文
     */
    private BootstrapApplicationContext applicationContext;

    /**
     * 记录verticle的部署id
     * k:class v:VerticleInfo
     */
    private final Map<Class<? extends BaseVerticle>, VerticleInfo> verticleIdMap = new ConcurrentHashMap<>();

    /**
     * 停服处理器
     */
    private AtomicReference<Future<Void>> stopFuture;

    /**
     * verticle创建初始化
     *
     * @param vertx   the deploying Vert.x instance
     * @param context the context of the verticle
     */
    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        //注册事件源
        context.config().put(EVENT_SOURCE_FLAG, CommonEventSource.class.getName());
        //转换为bootstrap
        this.applicationContext = (BootstrapApplicationContext) super.applicationContext;
    }

    /**
     * 同步启服逻辑
     */
    @Override
    public void start() {

    }

    /**
     * 异步启服逻辑
     *
     * @return
     */
    @Override
    public Future<Void> open() {
        //处理其他额外信息

        //监听配置类改变
        listenApplicationConfigUpdate();

        //根据优先级部署
        return deployVerticles()
                .transform(ar -> {
                    if (ar.failed()) {
                        log.error("部署Verticles失败", ar.cause());
                        //开始停服
                        applicationContext.publishEvent(new BootstrapVerticleStopEvent());
                        return Future.failedFuture(ar.cause());
                    }
                    log.info("部署Verticles完成！！！");
                    return Future.succeededFuture();
                });
    }

    /**
     * 监听服务配置改变
     */
    private void listenApplicationConfigUpdate() {
        ApplicationConfig.AutoUpdateApplicationConfig autoUpdateServerConfig =
                applicationContext.getApplicationConfig().getAutoUpdateServerConfig();
        if (!autoUpdateServerConfig.isEnable()) {
            return;
        }
        //设置监听更新时间
        Long updateTime = autoUpdateServerConfig.getUpdateTime();
        TimeUnit timeUnit = autoUpdateServerConfig.getTimeUnit();
        ConfigRetrieverOptions configRetrieverOptions = applicationContext.getConfigRetrieverOptions();
        configRetrieverOptions.setScanPeriod(timeUnit.toMillis(updateTime));
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);
        //先获取一次配置
        configRetriever.getConfig();
        configRetriever.listen(configChange -> {
            JsonObject newConfiguration = configChange.getNewConfiguration();
            log.warn("【注意检查是否修改了不能更新的配置项】监听到服务配置变化,newConfig:{}",
                    newConfiguration);
            //通过事件推送
            ApplicationEventUtils.publishEvent(new ApplicationConfigUpdateEvent(newConfiguration));
        });
    }

    /**
     * 部署verticles
     *
     * @return
     */
    private Future<Void> deployVerticles() {
        AtomicReference<Future<Void>> verticleFuture = new AtomicReference<>(Future.succeededFuture());
        log.info("开始部署Verticles,总量：{}", applicationContext.getVerticleClassTreeMap().size());
        applicationContext.getVerticleClassTreeMap()
                .forEach((order, verticleClassDefinitions) ->
                        verticleFuture.updateAndGet(future ->
                                future.compose(ignore -> {
                                            List<Future<Object>> futures = verticleClassDefinitions.stream()
                                                    .map(verticleClassDefinition ->
                                                            deployVerticle(order, verticleClassDefinition)
                                                    )
                                                    .collect(Collectors.toList());
                                            return Future.all(futures)
                                                    .mapEmpty();
                                        }
                                )
                        )
                );

        return verticleFuture.get();
    }

    /**
     * 部署verticle
     *
     * @param order                   优先级
     * @param verticleClassDefinition verticle类定义
     * @return
     */
    private Future<Object> deployVerticle(Integer order, BaseVerticleClassDefinition verticleClassDefinition) {
        //通过工厂来获取,多实例的verticle不能是单例的
        Class<? extends BaseVerticle> verticleClass = verticleClassDefinition.getClazz();
        //获取部署配置，没有则使用基础配置(注：引导verticle的配置实例数自动修正为1,其他的还是配置多少是多少)
        VerticleBaseConfig verticleBaseConfig = verticleBaseConfigMap
                .getOrDefault(verticleClassDefinition.getDeployConfigName(),
                        BeanUtil.copyProperties(applicationContext.getApplicationConfig(), VerticleBaseConfig.class));
        DeploymentOptions deploymentOptions = verticleBaseConfig.getDeploymentOptions();
        //添加自定义的信息到各自的verticle的context中
        JsonObject config = deploymentOptions.getConfig();
        if (config == null) {
            config = new JsonObject();
            deploymentOptions.setConfig(config);
        }
        config.put(EVENT_SOURCE_FLAG, verticleClassDefinition.getEventSource().getName());
        return vertx.deployVerticle(deployVerticleFactory.getVerticle(verticleClassDefinition), deploymentOptions)
                .compose(deploymentId -> {
                    VerticleInfo verticleInfo = new VerticleInfo();
                    verticleInfo.setClazz(verticleClass);
                    verticleInfo.setDeploymentId(deploymentId);
                    verticleInfo.setDeploymentOptions(deploymentOptions);
                    verticleIdMap.put(verticleClass, verticleInfo);
                    log.info("优先级: 【{}】  Verticle: {} 部署完成,实例数：{},部署Id: {}", order, verticleClass.getSimpleName(),
                            deploymentOptions.getInstances(), deploymentId);
                    return Future.succeededFuture();
                })
                .mapEmpty();
    }

    /**
     * 异步停服逻辑
     *
     * @throws Exception
     */
    @Override
    public Future<Void> close() {
        if (stopFuture == null) {
            log.error("非正常停服");
            return Future.succeededFuture();
        }
        return stopFuture.get();
    }

    /**
     * 执行停服
     *
     * @param order                    优先级
     * @param verticleClassDefinitions Verticle定义
     * @param deliveryOptions          eventbus请求配置
     * @return
     */
    private Future<Void> handleVerticleStop(Integer order, List<BaseVerticleClassDefinition> verticleClassDefinitions,
                                            DeliveryOptions deliveryOptions) {
        List<Future<Object>> stopFutureList = verticleClassDefinitions.stream()
                .map(verticleClassDefinition -> {
                    Class<? extends BaseVerticle> verticleClass = verticleClassDefinition.getClazz();
                    VerticleInfo verticleInfo = verticleIdMap.get(verticleClass);
                    if (verticleInfo == null) {
                        log.warn("停服失败,优先级：【{}】,Verticle: {} 已自主停服", order, verticleClass.getSimpleName());
                        return Future.succeededFuture();
                    }
                    //获取部署的实例数
                    int instances = verticleInfo.getDeploymentOptions().getInstances();
                    String deploymentId = verticleInfo.getDeploymentId();
                    log.info("停服开始,优先级：【{}】,当前停服的Verticle: {},实例数: {}, 部署Id: {}",
                            order, verticleClass.getSimpleName(), instances, deploymentId);
                    List<Future<Object>> futures = Stream.iterate(0, i -> i + 1)
                            .limit(instances)
                            .map(index -> {
                                Future<Message<Object>> request = eventBus.request(getStopEventBusAddr(deploymentId),
                                        JsonObject.of(verticleClass.getSimpleName(), "可以执行停服"), deliveryOptions);
                                return request.transform(ar -> {
                                    if (ar.failed()) {
                                        //停服失败不影响继续停服
                                        log.error("停服失败,优先级：【{}】,当前停服的Verticle: {},部署Id: {}",
                                                order, verticleClass.getSimpleName(), deploymentId, ar.cause());
                                    }
                                    return Future.succeededFuture();
                                });
                            })
                            .collect(Collectors.toList());
                    return Future.all(futures)
                            .compose(ignore -> {
                                //全部都停服完成后，广播一次防止通信失败的情况
                                eventBus.publish(getStopEventBusAddr(deploymentId),
                                        JsonObject.of(verticleClass.getSimpleName(), "停服广播"));
                                return Future.succeededFuture();
                            })
                            .mapEmpty();
                })
                .toList();
        return Future.all(stopFutureList)
                .mapEmpty();
    }

    /**
     * 同步停服逻辑
     * 没有则不管，异步的优先于同步
     */
    @Override
    public void stop() {

    }

    /**
     * 监听本地Verticle停止事件
     *
     * @param event Verticle停止事件
     */
    @EventListener(local = true)
    public void listenVerticleStop(VerticleStopEvent event) {
        verticleIdMap.remove(event.getClazz());
    }

    /**
     * 监听整体停服事件
     *
     * @param event Verticle停止事件
     */
    @EventListener(local = true)
    public synchronized void listenBootstrapVerticleStop(BootstrapVerticleStopEvent event) {
        //已经在执行了
        if (stopFuture != null) {
            return;
        }
        log.info("开始停服时间:{}", TimeUtils.getDateFormat(TimeUtils.toDateTime(event.getTimestamp())));
        this.stopFuture = new AtomicReference<>(Future.succeededFuture());
        //按照启动顺序的逆序取消部署
        DeliveryOptions deliveryOptions = new DeliveryOptions();
        //指定时间内没有停止完成就不等待了，继续下面的停服处理【整体固定2分钟内没有停服完成就会直接超时退出的】
        deliveryOptions.setSendTimeout(TimeUnit.SECONDS.toMillis(25));
        TreeMap<Integer, List<BaseVerticleClassDefinition>> verticleClassTreeMap =
                applicationContext.getVerticleClassTreeMap();
        verticleClassTreeMap.descendingMap()
                .forEach((order, verticleClassDefinitions) ->
                        stopFuture.getAndUpdate(s ->
                                s.compose(ignore ->
                                        //执行停服操作
                                        handleVerticleStop(order, verticleClassDefinitions, deliveryOptions)
                                )
                        )
                );
    }

}
