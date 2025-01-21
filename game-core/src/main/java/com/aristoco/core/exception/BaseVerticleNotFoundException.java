package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description 未找到引导verticle
 **/
public class BaseVerticleNotFoundException extends RuntimeException{

    public BaseVerticleNotFoundException() {
        super("未找到verticle");
    }

    public BaseVerticleNotFoundException(Throwable cause) {
        super("未找到verticle", cause);
    }
}
