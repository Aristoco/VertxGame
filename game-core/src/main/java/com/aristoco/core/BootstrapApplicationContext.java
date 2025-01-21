package com.aristoco.core;

import com.aristoco.core.bean.definition.BaseVerticleClassDefinition;
import com.aristoco.core.config.ApplicationConfig;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.TreeMap;

/**
 * @author chenguowei
 * @date 2024/6/14
 * @description Bootstrap上下文
 **/
@Slf4j
public class BootstrapApplicationContext extends GameApplicationContext {

    /**
     * 保存需要启动的verticle，并且根据优先级排序
     * k:order v:启动类集合
     */
    @Getter
    private final TreeMap<Integer, List<BaseVerticleClassDefinition>> verticleClassTreeMap = new TreeMap<>(Integer::compareTo);

    /**
     * 系统配置读取设置，可用于后续监听配置改变
     * <p> 注：因vertx加载CLASSPATH下的资源是会进行缓存,所以外部配置文件想要更新不要加入到CLASSPATH下
     */
    @Getter
    @Setter
    private ConfigRetrieverOptions configRetrieverOptions;

    /**
     * 应用启动配置
     */
    @Getter
    @Setter
    private ApplicationConfig applicationConfig;

    /**
     * 当前环境配置
     */
    @Getter
    @Setter
    private JsonObject applicationProfileConfigJson;
}
