package com.aristoco.core.annotation;

import cn.hutool.core.annotation.AliasFor;
import com.aristoco.core.bean.CommonEventSource;
import com.google.inject.Module;

import java.lang.annotation.*;

/**
 * @author chenguowei
 * @date 2024/6/14
 * @description Verticle注解
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface DeployVerticle {

    /**
     * 当前组件名字
     */
    @AliasFor(
            annotation = Component.class,
            attribute = "value"
    )
    String value() default "";

    /**
     * 加载优先级,值越大优先级越高,同级并行,不同级异步等待上级部署完成
     */
    int order() default 0;

    /**
     * verticle部署的配置类名
     * 如果指定了名字则使用指定的名字，默认使用的是类名首字母小写的驼峰格式
     */
    String deployConfigName() default "";

    /**
     * 事件源(用于区分事件来源)
     */
    Class<?> eventSource() default CommonEventSource.class;

    /**
     * 需要加载的guice模块
     */
    Class<Module>[] extraGuiceModule() default {};

    /**
     * 要排除的扫描的类型
     */
    Class<?>[] excludeClasses() default {};

    /**
     * 要排除扫描的路径或
     */
    String[] excludePackageName() default {};

    /**
     * 需要包含的包名
     */
    String[] includePackageName() default {};

    /**
     * 需要包含的类型
     */
    Class<?>[] includePackageClasses() default {};
}
