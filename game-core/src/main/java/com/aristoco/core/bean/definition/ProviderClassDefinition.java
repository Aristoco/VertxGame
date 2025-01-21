package com.aristoco.core.bean.definition;

import com.google.inject.Provider;
import lombok.Data;

/**
 * @author chenguowei
 * @date 2024/6/24
 * @description guice Provider实现类定义
 * 后续用于guice注入管理
 **/
@Data
public class ProviderClassDefinition {

    /**
     * 当前要管理的类
     */
    private Class<? extends Provider<?>> clazz;

    /**
     * 当前要管理的类名
     */
    private String beanName;

    /**
     * 是否是单例
     */
    private boolean isSingleton;

}
