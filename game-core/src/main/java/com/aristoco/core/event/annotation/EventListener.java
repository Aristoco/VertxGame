package com.aristoco.core.event.annotation;

import java.lang.annotation.*;

/**
 * 事件监听注解,被标记的方法参数会作为事件监听类型
 * @author Administrator
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EventListener {

    /**
     * 要监听的事件类列表
     * 可用于监听事件而不需要对应的事件参数
     * 特别：
     * 1、设置了当前参数，那么注解方法的参数列表和注解中的类不同的事件将不会被监听
     * 2、可以监听非事件的子类,需要发布时发布对应的类型
     */
    Class<?>[] value() default {};

    /**
     * 是否独立，即单独注册一个监听
     */
    boolean alone() default false;

    /**
     * 是否只消费本地vertx事件
     */
    boolean local() default false;

    /**
     * mvel表达式
     * 如果对应的事件在方法的参数列表中则使用参数列表的名字，
     * 否则使用的是事件类首字母小写的驼峰命名
     */
    String condition() default "";

    /**
     * 支持的事件源
     * 默认匹配所有的事件源,添加了事件源就只会监听指定事件源的
     * 所有Verticle未设置自定义事件源时，都是CommonEventSource
     * @see com.aristoco.core.bean.CommonEventSource
     */
    Class<?>[] eventSources() default {};
}
