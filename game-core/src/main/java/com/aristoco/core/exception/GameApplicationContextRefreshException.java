package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description 上下文刷新
 **/
public class GameApplicationContextRefreshException extends RuntimeException{

    public GameApplicationContextRefreshException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameApplicationContextRefreshException(Throwable cause) {
        super("程序启动失败，应用上下刷新失败",cause);
    }
}
