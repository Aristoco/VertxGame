package com.aristoco.core.config;

import io.vertx.core.VertxOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author chenguowei
 * @date 2024/6/25
 * @description vertx基础配置类,需要额外的启动vertx继承此配置
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class VertxBaseConfig extends VerticleBaseConfig{

    /**
     * vertx相关设置
     */
    private VertxOptions vertxOptions = new VertxOptions();

}
