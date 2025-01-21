package com.aristoco.core.constant;

/**
 * @author chenguowei
 * @date 2024/6/21
 * @description 应用常量
 **/
public interface GameApplicationConstants {

    /**
     * 应用上下文名
     */
    String GAME_APPLICATION_CONTEXT_NAME = "gameApplicationContext";

    /**
     * 启动配置前缀
     */
    String GAME_APPLICATION_CONFIG_PREFIX = "vertx";

    /**
     * guice依赖加载器
     */
    String GUICE_INJECTOR_NAME = "guiceInjector";

    /**
     * 基础要扫描的包路径
     * 加载框架的类
     */
    String BASE_SCAN_PACKAGE_PATH = "com.aristoco";
}
