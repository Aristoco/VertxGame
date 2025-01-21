package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description 不支持的类型错误
 **/
public class UnsupportedTypeException extends RuntimeException{

    public UnsupportedTypeException(String message) {
        super(message);
    }
}
