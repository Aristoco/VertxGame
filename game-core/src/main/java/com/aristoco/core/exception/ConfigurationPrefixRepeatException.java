package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description 配置类前缀重复
 **/
public class ConfigurationPrefixRepeatException extends RuntimeException {

    public ConfigurationPrefixRepeatException(String prefix, String className, String repeatClassName) {
        super("配置绑定类前缀重复，请检查重复的前缀名：" + prefix + " ,重复的类：" + className + " -- " + repeatClassName);
    }
}
