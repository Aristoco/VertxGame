package com.aristoco.core.bean.definition;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

/**
 * @author chenguowei
 * @date 2024/7/16
 * @description 事件监听器类定义信息
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class EventListenerBeanClassDefinition extends BeanClassDefinition {

    /**
     * 监听的事件类列表
     */
    private Set<Class<?>> listenerClasses;

    /**
     * 当前方法参数的类定义，也维护参数索引顺序
     */
    private List<ParameterBeanClassDefinition> parameterBeanClassDefinitions;

    /**
     * 是否独立监听器
     */
    private boolean alone = false;

    /**
     * 是否只监听本地vertx事件
     */
    private boolean local = false;

    /**
     * 是否是payloadEvent包装,用于监听器接口
     */
    private boolean payloadEvent = false;

    /**
     * 事件监听接口实现类的事件参数名
     */
    private String parameterName;

    /**
     * 事件条件
     */
    private String condition;

    /**
     * 事件监听的源列表
     */
    private Set<String> eventSources;

    /**
     * 参数类定义
     */
    @Data
    public static class ParameterBeanClassDefinition{

        /**
         * 参数类型
         */
        private Class<?> parameterClass;

        /**
         * 参数名字
         */
        private String parameterName;

        /**
         * 是否是payloadEvent包装
         */
        private boolean payloadEvent = false;

    }
}
