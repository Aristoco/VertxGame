package com.aristoco.core.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

/**
 * Verticle部署失败处理
 * @author Administrator
 */
@FunctionalInterface
public interface VerticleDeployFailedHandle {
    void handle(Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause);
}