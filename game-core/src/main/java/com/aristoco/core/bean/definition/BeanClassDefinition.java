package com.aristoco.core.bean.definition;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * @author chenguowei
 * @date 2024/6/24
 * @description 类的定义信息
 *
 **/
@Data
public class BeanClassDefinition {

    /**
     * 当前要管理的类
     */
    private Class<?> clazz;

    /**
     * 当前要管理的类名
     */
    private String beanName;

    /**
     * 是否是单例
     */
    private boolean isSingleton;

    /**
     * 是否是主要的
     */
    private boolean isPrimary;

    /**
     * 当前要访问的方法
     */
    private Method method;
}
