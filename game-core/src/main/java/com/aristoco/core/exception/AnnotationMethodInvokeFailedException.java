package com.aristoco.core.exception;

/**
 * @author chenguowei
 * @date 2024/7/15
 * @description 注解方法执行失败
 **/
public class AnnotationMethodInvokeFailedException extends RuntimeException {

    /**
     * 注解名
     */
    private final String annotationName;

    private final String methodName;

    public AnnotationMethodInvokeFailedException(String annotationName, String methodName, Throwable e) {
        super("执行注解方法失败. 注解:" + annotationName + ",方法:" + methodName, e);
        this.annotationName = annotationName;
        this.methodName = methodName;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public String getMethodName() {
        return methodName;
    }
}
