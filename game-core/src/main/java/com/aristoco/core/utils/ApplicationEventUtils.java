package com.aristoco.core.utils;

import com.aristoco.core.GameApplicationContext;
import com.aristoco.core.GameApplicationContextFactory;
import com.aristoco.core.event.ApplicationEvent;
import com.aristoco.core.vertx.verticle.BaseVerticle;
import io.vertx.core.Vertx;

/**
 * @author chenguowei
 * @date 2024/7/23
 * @description 事件工具类
 **/
public class ApplicationEventUtils {

    /**
     * 获取当前数据源
     *
     * @return
     */
    public static String getCurrentEventSource() {
        return Vertx.currentContext().config().getString(BaseVerticle.EVENT_SOURCE_FLAG);
    }

    /**
     * 发送事件
     *
     * @param event 事件
     */
    public static void publishEvent(ApplicationEvent event) {
        publishEvent(event, false);
    }

    /**
     * 发送事件
     *
     * @param event  事件
     * @param direct 是否点对点【点对点模式将会只有一个消费者接收到】
     */
    public static void publishEvent(ApplicationEvent event, boolean direct) {
        publishEvent((Object) event, direct);
    }

    /**
     * 发送事件
     *
     * @param event 事件
     */
    public static void publishEvent(Object event) {
        publishEvent(event, false);
    }

    /**
     * 发送事件
     *
     * @param event  事件
     * @param direct 是否点对点【点对点模式将会只有一个消费者接收到】
     */
    public static void publishEvent(Object event, boolean direct) {
        GameApplicationContext context = GameApplicationContextFactory.getCurrentGameApplicationContext();
        context.publishEvent(event, direct);
    }
}
