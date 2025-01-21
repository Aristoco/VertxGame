package com.aristoco.core.vertx;

/**
 * vertx停止后处理
 * @author Administrator
 */
@FunctionalInterface
public interface VertxAfterStopHandle {
    void handle();
}