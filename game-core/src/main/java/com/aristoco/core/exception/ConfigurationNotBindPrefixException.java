package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description 配置类未绑定前缀
 **/
public class ConfigurationNotBindPrefixException extends RuntimeException {

    /**
     * 配置类名
     */
    private final String className;

    public ConfigurationNotBindPrefixException(String className) {
        super("配置绑定类前缀未配置,类名：" + className);
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
