package com.aristoco.core.guice.provider;

import cn.hutool.core.util.StrUtil;
import com.aristoco.core.GameApplicationContext;
import com.aristoco.core.bean.definition.ConfigurationBeanClassDefinition;
import com.aristoco.core.exception.ConfigurationNotFoundException;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * @author chenguowei
 * @date 2024/6/21
 * @description @Bean注解产生的类的加载方法
 **/
@Slf4j
public class ConfigurationBeanClassProvider<T> implements Provider<T> {

    /**
     * 应用上下文
     */
    private final GameApplicationContext gameApplicationContext;

    /**
     * 配置类定义
     */
    private final ConfigurationBeanClassDefinition beanClassDefinition;

    public ConfigurationBeanClassProvider(GameApplicationContext gameApplicationContext,
                                          ConfigurationBeanClassDefinition beanClassDefinition) {
        this.gameApplicationContext = gameApplicationContext;
        this.beanClassDefinition = beanClassDefinition;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        Injector injector = gameApplicationContext.getInjector();

        Object instance;

        Class<?> configurationClass = beanClassDefinition.getConfigurationClass();
        String configurationBeanName = beanClassDefinition.getConfigurationBeanName();
        if (StrUtil.isNotBlank(configurationBeanName)) {
            Binding<?> binding = injector
                    .getExistingBinding(Key.get(configurationClass,
                            Names.named(configurationBeanName)));
            if (binding == null) {
                throw new ConfigurationNotFoundException(configurationBeanName, configurationClass.getSimpleName());
            } else {
                instance = binding.getProvider().get();
            }
        } else {
            instance = injector.getInstance(configurationClass);
        }

        Method method = beanClassDefinition.getMethod();
        try {
            return (T) method.invoke(instance);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
