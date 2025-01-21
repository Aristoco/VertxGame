package com.aristoco.core.bean.definition;

import com.aristoco.core.event.ApplicationEventMulticaster;
import lombok.Data;

/**
 * @author chenguowei
 * @date 2024/6/24
 * @description ApplicationEventMulticaster类定义信息
 **/
@Data
public class ApplicationEventMulticasterClassDefinition {

    /**
     * 当前要管理的类
     */
    private Class<? extends ApplicationEventMulticaster> clazz;

    /**
     * 当前要管理的类名
     */
    private String beanName;

    /**
     * 是否是单例
     */
    private boolean isSingleton;

}
