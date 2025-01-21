package com.aristoco.core.annotation;

import com.google.inject.Module;

import java.lang.annotation.*;

/**
 * @author chenguowei
 * @date 2024/6/14
 * @description 游戏启动类标识
 **/
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface VertxGameApplication {

    /**
     * 要排除的扫描的类型
     */
    Class<?>[] excludeClasses() default {};

    /**
     * 要排除扫描的路径或者名字,支持通配符
     */
    String[] excludePackageName() default {};

    /**
     * 需要扫描的包名
     */
    String[] scanBasePackages() default {};

    /**
     * 需要扫描的类型
     */
    Class<?>[] scanBasePackageClasses() default {};

    /**
     * 需要加载的guice模块
     * 注:如果通过启动类已经加载，则不会加载当前的，需要有无参构造函数，如需有参在启动类注入
     */
    Class<Module>[] extraGuiceModule() default {};
}
