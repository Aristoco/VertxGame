package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description guice module初始化失败
 **/
public class GuiceModuleInitException extends RuntimeException {

    public GuiceModuleInitException(String moduleClass, Throwable cause) {
        super("实例化guice的module失败，失败的module:" + moduleClass, cause);
    }
}
