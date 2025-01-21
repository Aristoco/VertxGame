package com.aristoco.core.annotation;

import java.lang.annotation.*;

/**
 * @author chenguowei
 * @date 2024/6/18
 * @description 原型注解
 **/
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Prototype {
}
