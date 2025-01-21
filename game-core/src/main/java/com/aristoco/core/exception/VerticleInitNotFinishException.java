package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/8/2
 * @description
 **/
public class VerticleInitNotFinishException extends RuntimeException{

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public VerticleInitNotFinishException() {
        super("Verticle还未初始化完成");
    }
}
