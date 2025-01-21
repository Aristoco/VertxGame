package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description guice provider重复
 **/
public class GuiceProviderRepeatException extends RuntimeException {

    public GuiceProviderRepeatException(String className, String providerName, String repeatProviderName) {
        super("guice provider提供多个相同类型的,请检查代码,重复注入类型:" + className + ", "
                + providerName + " -- " + repeatProviderName);
    }
}
