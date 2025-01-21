package com.aristoco.core.vertx.factory;

import com.aristoco.core.annotation.Component;
import com.aristoco.core.bean.definition.BaseVerticleClassDefinition;
import com.aristoco.core.guice.GuiceVerticleFactory;

/**
 * @author chenguowei
 * @date 2024/6/25
 * @description 部署verticle的生成工厂
 **/
@Component
public class DeployVerticleFactory {

    /**
     * 获取指定的部署verticle,如果是多实例的verticle需要添加@Prototype注解
     *
     * @param verticleClassDefinition verticle类定义
     * @return
     */
    public String getVerticle(BaseVerticleClassDefinition verticleClassDefinition) {
        return GuiceVerticleFactory.getGuiceVerticleName(verticleClassDefinition.getClazz());
    }

}
