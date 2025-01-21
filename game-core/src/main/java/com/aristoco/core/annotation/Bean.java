package com.aristoco.core.annotation;

import java.lang.annotation.*;

/**
 * @author chenguowei
 * @date 2024/6/18
 * @description bean注解
 * 用于配置类中声明提供的依赖[便于集成其他组件]
 * 通过配置类注入的只是单例的，不会被多实现的依赖注入管理【后续考虑】
 **/
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

    /**
     * bean的名字
     * @return
     */
    String value() default "";

    /**
     * bean的名字
     */
    String name() default "";

}
