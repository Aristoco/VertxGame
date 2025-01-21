package com.aristoco.core.bean.definition;

import com.aristoco.core.vertx.verticle.BaseVerticle;
import lombok.Data;

/**
 * @author chenguowei
 * @date 2024/6/24
 * @description 服务verticle类定义信息
 **/
@Data
public class BaseVerticleClassDefinition {

    /**
     * 当前要管理的类
     */
    private Class<? extends BaseVerticle> clazz;

    /**
     * 当前要管理的类名
     */
    private String beanName;

    /**
     * 是否是单例
     */
    private boolean isSingleton;

    /**
     * 部署的配置名
     */
    private String deployConfigName;

    /**
     * 事件源标记
     */
    private Class<?> eventSource;

}
