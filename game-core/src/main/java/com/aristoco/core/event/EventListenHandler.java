package com.aristoco.core.event;

/**
 * @author chenguowei
 * @date 2024/7/30
 * @description 事件监听执行器
 **/
@FunctionalInterface
public interface EventListenHandler<T> {

    void accept(T t) throws Throwable;

}
