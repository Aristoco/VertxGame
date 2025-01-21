package com.aristoco.core.event;

import com.aristoco.core.vertx.verticle.BaseVerticle;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author chenguowei
 * @date 2024/7/31
 * @description verticle停止事件
 **/
@NoArgsConstructor
@Getter
public class VerticleStopEvent extends ApplicationEvent{

    /**
     * 停服的Verticle类型
     */
    private Class<? extends BaseVerticle> clazz;

    /**
     * 创建一个事件
     */
    public VerticleStopEvent(Class<? extends BaseVerticle> clazz) {
        this.clazz = clazz;
    }
}
