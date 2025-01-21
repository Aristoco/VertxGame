package com.aristoco.core.vertx.verticle;

import com.aristoco.core.BootstrapVerticle;
import com.aristoco.core.GameApplicationContext;
import com.aristoco.core.event.BootstrapVerticleStopEvent;
import com.aristoco.core.event.VerticleStopEvent;
import com.aristoco.core.utils.StringUtils;
import com.aristoco.core.vertx.VerticleStopHandler;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.SharedData;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author chenguowei
 * @date 2024/6/13
 * @description 基础verticle
 **/
@Slf4j
public abstract class BaseVerticle extends AbstractVerticle {

    /**
     * 注入通用的上下文
     */
    @Inject
    public GameApplicationContext applicationContext;

    /**
     * 停服时的标记,通过eventbus通知不同的verticle
     */
    public static final String EVENTBUS_STOP_FLAG = "EVENTBUS.VERTX.STOP.FLAG.";

    /**
     * 事件源标志
     */
    public static final String EVENT_SOURCE_FLAG = "EVENT_SOURCE_FLAG";

    /**
     * 当前Verticle的类型
     */
    public static final String VERTICLE_CLASS_NAME = "VERTICLE_CLASS_NAME";

    /**
     * 当前verticle的上下文名
     */
    public static final String VERTICLE_APPLICATION_CONTEXT_DISPLAY_NAME = "VERTICLE_APPLICATION_CONTEXT_DISPLAY_NAME";

    /**
     * 获取共享数据结构(缓存)
     */
    public SharedData sharedData;

    /**
     * 获取eventbus
     */
    public EventBus eventBus;

    /**
     * 需要按需关闭的操作
     */
    private final TreeMap<Integer, List<VerticleStopHandler>> stopHandlerMap = new TreeMap<>();

    /**
     * 最终关闭操作
     */
    private VerticleStopHandler finalStopHandler;

    /**
     * 初始化verticle
     *
     * @param vertx   the deploying Vert.x instance
     * @param context the context of the verticle
     */
    @Override
    public void init(Vertx vertx, Context context) {
        //todo 代理初始化方法
        super.init(vertx, context);
        this.sharedData = vertx.sharedData();
        this.eventBus = vertx.eventBus();
        //注册当前Verticle名
        context.put(VERTICLE_CLASS_NAME, this.getClass().getName());
        //注册上下文名
        context.put(VERTICLE_APPLICATION_CONTEXT_DISPLAY_NAME, applicationContext.getDisplayName());
        //上下文预刷新
        applicationContext.prepareRefresh();
    }

    @Override
    public final void start(Promise<Void> startPromise) throws Exception {
        //上下文刷新
        applicationContext.onRefresh();
        //优先执行异步启服逻辑，在执行同步
        open().onSuccess(ignore -> {
                    try {
                        super.start(startPromise);
                        //注册停服处理
                        registerVerticleStop();
                        //上下文完成刷新
                        applicationContext.finishRefresh();
                    } catch (Exception e) {
                        startPromise.fail(e);
                    }
                })
                .onFailure(startPromise::fail);
    }

    /**
     * 同步启服逻辑
     */
    @Override
    public abstract void start();

    /**
     * 异步启服逻辑，异步优先同步
     * 不需要处理直接返回成功的future
     * 例如: Future.succeededFuture()
     */
    public abstract Future<Void> open();

    /**
     * verticle关闭方法
     *
     * @param stopPromise 停服处理器
     */
    @Override
    public final void stop(Promise<Void> stopPromise) {
        log.info("触发停服,Verticle:{}", this.getClass().getSimpleName());
        //代理关闭流程
        this.finalStopHandler = () -> close().onComplete(ar -> {
            if (ar.failed()) {
                log.error("verticle关闭时出错", ar.cause());
                stopPromise.fail(ar.cause());
                return;
            }
            try {
                stop();
                //推送停服事件
                applicationContext.publishEvent(new VerticleStopEvent(this.getClass()));
                stopPromise.complete();
            } catch (Exception e) {
                log.error("verticle停止时出错", e);
                stopPromise.fail(e);
            }
        });

        //关闭时会先执行由BootstrapVerticle启动的其他Verticle，最后执行BootstrapVerticle的
        if (BootstrapVerticle.class.isAssignableFrom(this.getClass())) {
            //都停服完成后，最后bootstrap开始停服
            eventBus.publish(getStopEventBusAddr(), JsonObject.of(this.getClass().getSimpleName(), "开始停服"));
        } else {
            //推送停服事件给BootstrapVerticle开启对应的停服处理
            applicationContext.publishEvent(new BootstrapVerticleStopEvent());
        }

        //todo 测试用后续删除
        //vertx.setTimer(5000L, tid -> {
        //    log.warn("主动停服,Verticle: {}   超过20s未收到停服信息", this.getClass().getSimpleName());
        //    startVerticleStop();
        //});
    }

    /**
     * 异步停服逻辑
     * 不需要处理直接返回成功的future
     * 例如: Future.succeededFuture()
     *
     * @return
     */
    public abstract Future<Void> close();

    /**
     * 同步停服逻辑
     * 没有则不管，异步的优先于同步
     */
    @Override
    public abstract void stop();

    /**
     * 获取停服监听的地址
     *
     * @return
     */
    protected String getStopEventBusAddr() {
        return getStopEventBusAddr(deploymentID());
    }

    /**
     * 获取停服监听的地址
     *
     * @return
     */
    protected String getStopEventBusAddr(String deploymentId) {
        //deploymentId 是带“-”不符合要求替换掉
        deploymentId = StringUtils.replace(deploymentId, "-", "");
        return EVENTBUS_STOP_FLAG + deploymentId;
    }

    /**
     * 添加停服处理器
     * <p> 当前停服处理在默认的异步和同步停服处理方法之前执行
     * <p> 可以指定优先级会按照优先级进行依次执行
     *
     * @param verticleStopHandler 停服处理器
     */
    public void registerVerticleStopHandler(VerticleStopHandler verticleStopHandler) {
        registerVerticleStopHandler(0, verticleStopHandler);
    }

    /**
     * 添加停服处理器
     * <p> 当前停服处理在默认的异步和同步停服处理方法之前执行
     * <p> 可以指定优先级会按照优先级进行依次执行
     *
     * @param order               优先级
     * @param verticleStopHandler 停服处理器
     */
    public void registerVerticleStopHandler(Integer order, VerticleStopHandler verticleStopHandler) {
        stopHandlerMap.computeIfAbsent(order, k -> new ArrayList<>())
                .add(verticleStopHandler);
    }

    /**
     * 执行事件
     */
    public void registerVerticleStop() {
        //监听停服消息
        MessageConsumer<JsonObject> consumer = eventBus.consumer(getStopEventBusAddr());
        consumer.handler(message -> {
            JsonObject body = message.body();
            log.info("执行停服,Verticle: {},收到的停服消息:{}", this.getClass().getSimpleName(), body.toString());

            startVerticleStop().onComplete(ar -> {
                //响应消息给引导verticle
                message.reply(JsonObject.of(this.getClass().getSimpleName(), "停服完成"));
                log.info("停服完成,Verticle: {}", this.getClass().getSimpleName());
                consumer.unregister();
            });

        });
    }

    /**
     * 开始停服
     *
     * @return
     */
    protected Future<Void> startVerticleStop() {
        AtomicReference<Future<Void>> stopOrderFuture = new AtomicReference<>(Future.succeededFuture());
        this.stopHandlerMap.forEach((order, stopHandlers) ->
                stopOrderFuture.updateAndGet(stopFuture ->
                        handleVerticleStopHandlers(order, stopHandlers, stopFuture)
                ));
        return stopOrderFuture.get()
                .compose(ignore -> this.finalStopHandler.handle())
                .mapEmpty();
    }

    /**
     * 执行停服处理器
     *
     * @param order        停服处理器优先级
     * @param stopHandlers 停服处理器列表
     * @param stopFuture   停服处理执行结果
     * @return
     */
    private Future<Void> handleVerticleStopHandlers(Integer order, List<VerticleStopHandler> stopHandlers, Future<Void> stopFuture) {
        return vertx.executeBlocking(() ->
                        stopFuture.compose(ignore -> {
                                    List<Future<Void>> stopFutures = stopHandlers.stream()
                                            .map(VerticleStopHandler::handle)
                                            .toList();
                                    log.info("当前关闭优先级：{},处理器数量：{}", order, stopHandlers.size());
                                    return Future.all(stopFutures)
                                            .transform(ar -> {
                                                if (ar.failed()) {
                                                    log.error("停服处理器执行失败", ar.cause());
                                                }
                                                return Future.succeededFuture();
                                            })
                                            .mapEmpty();
                                }
                        )
                , false).mapEmpty();
    }
}
