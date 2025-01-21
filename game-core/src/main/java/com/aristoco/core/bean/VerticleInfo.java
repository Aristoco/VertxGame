package com.aristoco.core.bean;

import com.aristoco.core.vertx.verticle.BaseVerticle;
import io.vertx.core.DeploymentOptions;
import lombok.Data;

/**
 * @author chenguowei
 * @date 2024/7/31
 * @description 记录部署的Verticle的信息
 **/
@Data
public class VerticleInfo {

    /**
     * 当前Verticle的类型
     */
    private Class<? extends BaseVerticle> clazz;

    /**
     * 当前Verticle的部署id
     */
    private String deploymentId;

    /**
     * 当前Verticle的部署信息
     */
    private DeploymentOptions deploymentOptions;

}
