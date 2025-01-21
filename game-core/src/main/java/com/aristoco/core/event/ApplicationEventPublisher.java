package com.aristoco.core.event;

/**
 * 事件发布器接口
 *
 * @author Administrator
 */
@FunctionalInterface
public interface ApplicationEventPublisher {

    /**
     * 发布事件
     *
     * @param event 事件
     */
    default void publishEvent(ApplicationEvent event) {
        publishEvent(event, false);
    }

    /**
     * 发布事件
     *
     * @param event  事件
     * @param direct 是否点对点【点对点模式将会只有一个消费者接收到】
     */
    default void publishEvent(ApplicationEvent event, boolean direct) {
        publishEvent((Object) event, direct);
    }

    /**
     * 发布事件
     *
     * @param event 事件
     */
    default void publishEvent(Object event) {
        publishEvent(event, false);
    }

    /**
     * 发布事件
     *
     * @param event  事件
     * @param direct 是否点对点【点对点模式将会只有一个消费者接收到】
     */
    void publishEvent(Object event, boolean direct);

}
