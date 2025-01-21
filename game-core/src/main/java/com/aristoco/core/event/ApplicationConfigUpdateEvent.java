package com.aristoco.core.event;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

/**
 * @author chenguowei
 * @date 2024/7/30
 * @description 服务器配置更新事件
 * 需要开启服务配置自动更新
 **/
@Getter
public class ApplicationConfigUpdateEvent extends ApplicationEvent{

    /**
     * 记录当前最新配置
     */
    private final JsonObject newConfig;

    /**
     * 创建一个事件
     */
    public ApplicationConfigUpdateEvent(JsonObject newConfig) {
        this.newConfig = newConfig;
    }


}
