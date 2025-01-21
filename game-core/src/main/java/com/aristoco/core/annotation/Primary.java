package com.aristoco.core.annotation;

import java.lang.annotation.*;

/**
 * @author chenguowei
 * @date 2024/6/14
 * @description 主要注解, 用于在接口多实现，类多继承中指定主要的那个
 * 注：就是适配guice中根据接口或者类注入时为使用@Named指定时的默认注入
 **/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {

}
