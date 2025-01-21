package com.aristoco.core.config;

import io.vertx.core.DeploymentOptions;
import lombok.Data;

/**
 * @author chenguowei
 * @date 2024/6/25
 * @description verticle相关配置,新增verticle都只继承此配置
 **/
@Data
public class VerticleBaseConfig {

    /**
     * 是否部署,默认部署的
     */
    private boolean enable = true;

    /**
     * verticle相关配置
     */
    private DeploymentOptions deploymentOptions = new DeploymentOptions();

}
