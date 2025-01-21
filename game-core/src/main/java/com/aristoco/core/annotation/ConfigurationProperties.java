package com.aristoco.core.annotation;

import cn.hutool.core.annotation.MirrorFor;

import java.lang.annotation.*;

/**
 * @author chenguowei
 * @date 2024/6/18
 * @description 配置绑定类, 需要配合 {@link Component} 注解使用
 **/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationProperties {

    /**
     * 当前配置的前缀，用于匹配对应的配置
     */
    @MirrorFor(attribute = "prefix")
    String value() default "";

    /**
     * 当前配置的前缀，用于匹配对应的配置
     */
    @MirrorFor(attribute = "value")
    String prefix() default "";

}
