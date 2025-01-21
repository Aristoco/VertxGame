package com.aristoco.core.config;

import io.vertx.config.ConfigStoreOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author chenguowei
 * @date 2024/7/5
 * @description 自定义配置中心【基于vertx支持的配置中心处理】
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class CustomConfigStoreOptions extends ConfigStoreOptions {

    /**
     * 当前配置中心是否启用
     */
    private boolean enable = false;

    /**
     * 配置中心加载顺序
     * 越小越在前,会被大的覆盖，相同则看获取配置顺序
     */
    private int order = 0;

}
