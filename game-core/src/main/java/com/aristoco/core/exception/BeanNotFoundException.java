package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description bean未找到
 **/
public class BeanNotFoundException extends RuntimeException {


    public BeanNotFoundException(String beanName, String className) {
        super("未找到对应的bean,beanName:" + beanName + ", className:" + className);
    }
}
