package com.aristoco.core.annotation;

import java.lang.annotation.*;

/**
 * @author chenguowei
 * @date 2024/6/14
 * @description 组件注解
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {

    /**
     * 当前组件名
     */
    String value() default "";

}
