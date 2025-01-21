package com.aristoco.core.annotation;

import cn.hutool.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author chenguowei
 * @date 2024/6/14
 * @description 请求注解
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {

    /**
     * 当前组件名字
     */
    @AliasFor(
            annotation = Component.class,
            attribute = "value"
    )
    String value() default "";

}
