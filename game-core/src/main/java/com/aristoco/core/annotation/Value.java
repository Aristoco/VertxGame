package com.aristoco.core.annotation;

import java.lang.annotation.*;

/**
 * @author chenguowei
 * @date 2024/6/18
 * @description 配置注入注解，支持mvel
 **/
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {

	/**
	 * 配置读取的表达式
	 * @see com.aristoco.core.utils.MvelUtils
	 */
	String value();

}