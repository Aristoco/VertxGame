package com.aristoco.core.bean.definition;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * @author chenguowei
 * @date 2024/7/16
 * @description 通过注解Value注入配置值的绑定类
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ValueConfigBindBeanClassDefinition extends BeanClassDefinition {

    /**
     * 预编译的表达式
     */
    private Serializable expression;

    /**
     * 字段类型
     */
    private Class<?> filedClass;

    /**
     * 私有属性访问
     */
    private Field field;

}
