package com.aristoco.core.bean.definition;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author chenguowei
 * @date 2024/6/24
 * @description 配置类的定义信息
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ConfigurationBeanClassDefinition extends BeanClassDefinition{

    /**
     * 配置类
     */
    private Class<?> configurationClass;

    /**
     * 配置类名
     */
    private String configurationBeanName;
}
