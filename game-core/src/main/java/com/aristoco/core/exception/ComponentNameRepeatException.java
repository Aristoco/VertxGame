package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description 组件名称重复
 **/
public class ComponentNameRepeatException extends RuntimeException {
    public ComponentNameRepeatException(String componentName, String className, String repeatClassName) {
        super("组件名称重复,组件名：" + componentName + ", " + className + " -- " + repeatClassName);
    }
}
