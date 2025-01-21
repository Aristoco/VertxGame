package com.aristoco.core.event;

import java.util.function.Predicate;

/**
 * @author chenguowei
 * @date 2024/7/8
 * @description 事件多播器接口
 **/
public interface ApplicationEventMulticaster {

    /**
     * 添加事件监听器
     * @param listener the listener to add
     * @see #removeApplicationListener(ApplicationListener)
     * @see #removeApplicationListeners(Predicate)
     */
    default void addApplicationListener(ApplicationListener<?> listener){
        throw new UnsupportedOperationException();
    }


    /**
     * 移除事件监听器
     * @param listener the listener to remove
     * @see #addApplicationListener(ApplicationListener)
     * @see #removeApplicationListeners(Predicate)
     */
    default void removeApplicationListener(ApplicationListener<?> listener){
        throw new UnsupportedOperationException();
    }

    /**
     * 移除符合条件的事件
     * @see #addApplicationListener(ApplicationListener)
     * @see #removeApplicationListener(ApplicationListener)
     */
    default void removeApplicationListeners(Predicate<ApplicationListener<?>> predicate){
        throw new UnsupportedOperationException();
    }


    /**
     * 移除所有的事件监听器
     * @see #removeApplicationListeners(Predicate)
     */
    default void removeAllListeners(){
        throw new UnsupportedOperationException();
    }

    /**
     * 事件多播
     * @param event 事件
     */
    default void multicastEvent(ApplicationEvent event){
        multicastEvent(event,false);
    }

    /**
     * 事件多播
     * @param event 事件
     * @param direct 是否点对点【点对点模式将会只有一个消费者接收到】
     */
    void multicastEvent(ApplicationEvent event,boolean direct);
}
