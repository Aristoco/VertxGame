package com.aristoco.core.el;

import io.vertx.core.json.JsonObject;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;

/**
 * @author chenguowei
 * @date 2024/7/30
 * @description mvel2解析vertx json
 **/
public class JsonObjectPropertyHandler implements PropertyHandler {

    /**
     * 获取值
     *
     * @param s                       属性名
     * @param o                       对象
     * @param variableResolverFactory 参数解析工厂(用于解析属性中的额外参数,脚本相关处理)
     * @return
     */
    @Override
    public Object getProperty(String s, Object o, VariableResolverFactory variableResolverFactory) {
        if (o instanceof JsonObject jsonObject) {
            return jsonObject.getValue(s);
        }
        return null;
    }

    /**
     * 设置值
     *
     * @param s                       属性名
     * @param o                       对象
     * @param variableResolverFactory 参数解析工厂(用于解析属性中的额外参数,脚本相关处理)
     * @param o1                      属性值
     * @return
     */
    @Override
    public Object setProperty(String s, Object o, VariableResolverFactory variableResolverFactory, Object o1) {
        if (o instanceof JsonObject jsonObject) {
            return jsonObject.put(s, o1);
        }
        return null;
    }
}
