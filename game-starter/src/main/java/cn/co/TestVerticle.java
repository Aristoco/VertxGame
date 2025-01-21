package cn.co;

import cn.co.a.A;
import cn.co.a.T123;
import com.aristoco.core.annotation.DeployVerticle;
import com.aristoco.core.annotation.Prototype;
import com.aristoco.core.bean.CommonEventSource;
import com.aristoco.core.event.annotation.EventListener;
import com.aristoco.core.vertx.verticle.BaseVerticle;
import com.google.inject.Injector;
import io.vertx.core.Future;
import io.vertx.core.shareddata.AsyncMap;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author chenguowei
 * @date 2024/6/21
 * @description
 **/
@Slf4j
@DeployVerticle(deployConfigName = "123")
@Prototype
public class TestVerticle extends BaseVerticle {

    @Override
    public void start() {

        //MessageProducer<Object> publisher = vertx.eventBus().publisher("123");
        //publisher.write(new Object());
        //vertx.eventBus().publish("123",new Object());

        Future<AsyncMap<Object, Object>> asyncMap = vertx.sharedData().getAsyncMap("123");


        System.out.println("test verticle启动,id: " + deploymentID());

        //vertx.setPeriodic(1000L,t->{
        //    gameApplicationContext.publishEvent("456");
        //});

        //vertx.setTimer(1000L, t -> {
        //    for (int i = 0; i < 100; i++) {
        //        int finalI = i;
        //        vertx.executeBlocking(() -> {
        //            try {
        //                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        //            } catch (InterruptedException e) {
        //                throw new RuntimeException(e);
        //            }
        //            System.out.println("ser-" + finalI + "执行完成");
        //            return null;
        //        });
        //    }
        //});
        //
        //vertx.setTimer(1000L, t -> {
        //    for (int i = 0; i < 100; i++) {
        //        int finalI = i;
        //        vertx.executeBlocking(() -> {
        //            try {
        //                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        //            } catch (InterruptedException e) {
        //                throw new RuntimeException(e);
        //            }
        //            System.out.println("par-" + finalI + "执行完成");
        //            return null;
        //        }, false);
        //    }
        //});

        //vertx.setTimer(5000L, tid -> {
        //    vertx.undeploy(deploymentID());
        //});

        Injector injector = applicationContext.getInjector();
        T123 instance = injector.getInstance(T123.class);
        instance.asd();
    }

    /**
     * 异步启服逻辑，异步优先同步
     *
     * @return
     */
    @Override
    public Future<Void> open() {
        return Future.succeededFuture();
    }

    /**
     * 异步停服逻辑
     *
     * @return
     */
    @Override
    public Future<Void> close() {
        return Future.succeededFuture();
    }

    /**
     * 同步停服逻辑
     */
    @Override
    public void stop() {
        System.out.println("开始停止");
    }

    @EventListener(alone = true, eventSources = {A.class, CommonEventSource.class}, condition = "str == 123")
    public void testEvent(String str) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("收到事件信息：" + str);
    }

}
