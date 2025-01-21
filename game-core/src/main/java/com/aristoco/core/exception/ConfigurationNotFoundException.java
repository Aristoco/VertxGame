package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description 配置类未找到
 **/
public class ConfigurationNotFoundException extends RuntimeException {


    public ConfigurationNotFoundException(String configurationBeanName, String className) {
        super("配置类加载出错,未找到对应名称的配置类,配置类名:" + configurationBeanName +
                ",配置类:" + className);
    }
}
