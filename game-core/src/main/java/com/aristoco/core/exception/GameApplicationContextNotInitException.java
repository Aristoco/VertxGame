package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description 上下文未初始化
 **/
public class GameApplicationContextNotInitException extends RuntimeException{

    public GameApplicationContextNotInitException() {
        super("程序启动失败，应用上下文未初始化，请检查启动类");
    }

    public GameApplicationContextNotInitException(String message) {
        super(message);
    }

    public GameApplicationContextNotInitException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameApplicationContextNotInitException(Throwable cause) {
        super(cause);
    }
}
