package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description vertx启动配置未找到错误
 **/
public class VertxStartUpConfigNotFoundException extends RuntimeException{

    public VertxStartUpConfigNotFoundException() {
        super();
    }

    public VertxStartUpConfigNotFoundException(String message) {
        super(message);
    }

    public VertxStartUpConfigNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public VertxStartUpConfigNotFoundException(Throwable cause) {
        super(cause);
    }
}
