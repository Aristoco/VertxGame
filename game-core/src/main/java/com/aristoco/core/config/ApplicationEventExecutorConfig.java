package com.aristoco.core.config;

import com.aristoco.core.annotation.Component;
import com.aristoco.core.annotation.ConfigurationProperties;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.tracing.TracingPolicy;
import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * @author chenguowei
 * @date 2024/7/23
 * @description
 **/
@Data
@ConfigurationProperties(prefix = "vertx.event")
public class ApplicationEventExecutorConfig {

    /**
     * 事件线程池配置
     */
    private ExecutorConfig executor = new ExecutorConfig();

    /**
     * 事件eventbus配置
     */
    private EventBusOptions eventOptions = new EventBusOptions();

    /**
     * 事件的eventbus发送消息的配置
     */
    @Data
    public static class EventBusOptions {

        /**
         * 设置新的编码器
         */
        private String codecName;

        /**
         * 添加统一的headler
         */
        private JsonObject headers;

        /**
         * 集群模式下是否只将消息发个本地
         */
        private boolean localOnly;

        /**
         * 跟踪策略
         */
        private TracingPolicy tracingPolicy;

        /**
         * 获取eventbus发送配置
         * @return
         */
        public DeliveryOptions toDeliveryOptions(){
            return new DeliveryOptions(JsonObject.mapFrom(this));
        }
    }

    /**
     * 线程池配置
     */
    @Data
    public static class ExecutorConfig {
        /**
         * 是否启用线程池,默认不适用线程池
         */
        private boolean enable = false;

        /**
         * 是否按照顺序执行,默认按照顺序执行
         * 注：为true线程池中就只会启动一个线程执行
         */
        private boolean order = true;

        /**
         * 事件线程池名字
         */
        private String name = "eventbus-event";

        /**
         * 默认的线程数量
         * 注：order为true此项无效，为false时需要控制线程数量
         */
        private int poolSize = 2;

        /**
         * 单个任务最大执行时间
         * 注:超过这个时间会有日志警告
         */
        private long maxExecuteTime = 60L;

        /**
         * 单个任务最大执行时间单位
         */
        private TimeUnit maxExecuteTimeUnit = TimeUnit.SECONDS;
    }
}
