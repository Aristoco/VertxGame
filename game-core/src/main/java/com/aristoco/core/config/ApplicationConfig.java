package com.aristoco.core.config;

import com.aristoco.core.constant.GameApplicationConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author chenguowei
 * @date 2024/6/13
 * @description 应用配置
 * @see GameApplicationConstants#GAME_APPLICATION_CONFIG_PREFIX
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationConfig extends VertxBaseConfig {

    /**
     * 环境信息
     */
    private Profiles profiles = new Profiles();

    /**
     * 自定义配置中心，需要正确的类型才能解析
     */
    private List<CustomConfigStoreOptions> configStoreOptions = new ArrayList<>();

    /**
     * 服务信息
     */
    private Application application = new Application();

    /**
     * 服务时区信息
     */
    private ServerTimeZone timeZone = new ServerTimeZone();

    /**
     * 是否自动更新服务配置，默认false
     *
     */
    private AutoUpdateApplicationConfig autoUpdateServerConfig = new AutoUpdateApplicationConfig();

    /**
     * 自动更新服务配置信息
     */
    @Data
    public static class AutoUpdateApplicationConfig{

        /**
         * 是否开启，默认关闭
         * <p> 开启后，配置有更新会推送一个配置更新事件
         * @see com.aristoco.core.event.ApplicationConfigUpdateEvent
         */
        private boolean enable = false;

        /**
         * 检查配置更新的间隔时间,默认5分钟检查一次
         */
        private Long updateTime = 5L;

        /**
         * 检查配置更新的间隔时间单位
         */
        private TimeUnit timeUnit = TimeUnit.MINUTES;
    }

    /**
     * 时区信息
     */
    @Data
    public static class ServerTimeZone {

        /**
         * 时区偏移
         * 【-12~12】
         */
        private Integer timeOffset;

    }

    /**
     * 环境信息
     */
    @Data
    public static class Profiles {

        /**
         * 激活环境
         */
        private String active;

    }

    /**
     * 服务信息
     */
    @Data
    static class Application {

        /**
         * 应用名称
         */
        private String name;

        /**
         * 是否部署verticle
         * 默认部署。false:标识启动一个基础的vertx,用于高可用
         */
        private boolean enableVerticle = true;

    }

}
