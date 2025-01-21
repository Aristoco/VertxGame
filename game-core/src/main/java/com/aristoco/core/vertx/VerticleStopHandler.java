package com.aristoco.core.vertx;

import io.vertx.core.Future;

/**
 * @author chenguowei
 * @date 2024/7/17
 * @description
 **/
public interface VerticleStopHandler {

    Future<Void> handle();

}
