package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/8/2
 * @description
 **/
public class NotInVerticleContextException extends RuntimeException{

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public NotInVerticleContextException() {
        super("需要在Verticle环境中使用");
    }
}
