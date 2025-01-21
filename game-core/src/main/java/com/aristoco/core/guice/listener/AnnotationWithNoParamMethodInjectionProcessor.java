package com.aristoco.core.guice.listener;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import com.aristoco.core.exception.AnnotationMethodInvokeFailedException;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author chenguowei
 * @date 2024/3/8
 * @description 注解无参方法的guice注入监听器
 **/
public class AnnotationWithNoParamMethodInjectionProcessor implements TypeListener {

    private final Class<? extends Annotation> processAnnotation;

    public AnnotationWithNoParamMethodInjectionProcessor(Class<? extends Annotation> processAnnotation) {
        this.processAnnotation = processAnnotation;
    }

    /**
     * 判断是否有初始化方法，当依赖全部注入完成，执行初始化方法
     *
     * @param type      encountered by Guice
     * @param encounter context of this encounter, enables reporting errors, registering injection
     *                  listeners and binding method interceptors for {@code type}.
     * @param <I>
     */
    @Override
    public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
        Class<?> rawType = type.getRawType();
        for (Method method : ClassUtil.getDeclaredMethods(rawType)) {
            if (method.isAnnotationPresent(processAnnotation)) {
                encounter.register((InjectionListener<I>) instance -> {
                    invokeAnnotatedMethod(instance, method);
                });
            }
        }
    }

    /**
     * 通过反射调用对应的初始化方法
     * 【注：当前只支持无参方法】
     *
     * @param instance
     * @param method
     * @param <I>
     */
    private <I> void invokeAnnotatedMethod(I instance, Method method) {
        try {
            ReflectUtil.setAccessible(method);
            method.invoke(instance);
        } catch (Throwable e) {
            throw new AnnotationMethodInvokeFailedException(processAnnotation.getName(),
                    method.getName(), e);
        }finally {
            method.setAccessible(false);
        }
    }

}