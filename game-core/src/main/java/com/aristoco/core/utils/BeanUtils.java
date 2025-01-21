package com.aristoco.core.utils;

import cn.hutool.core.bean.BeanUtil;
import com.aristoco.core.GameApplicationContext;
import com.aristoco.core.GameApplicationContextFactory;
import io.vertx.core.json.JsonObject;

/**
 * @author chenguowei
 * @date 2024/7/24
 * @description bean工具
 **/
public class BeanUtils extends BeanUtil {

    /**
     * 获取对应的bean
     *
     * @param tClass bean的类型
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> tClass) {
        GameApplicationContext context = GameApplicationContextFactory.getCurrentGameApplicationContext();
        return context.getBean(tClass);
    }

    /**
     * 根据名字获取对应的bean
     *
     * @param beanName bean的名字
     * @param tClass   bean的类型
     * @param <T>
     * @return
     */
    public static <T> T getBean(String beanName, Class<T> tClass) {
        if (StringUtils.isBlank(beanName)) {
            return getBean(tClass);
        }
        GameApplicationContext context = GameApplicationContextFactory.getCurrentGameApplicationContext();
        return context.getBean(beanName, tClass);
    }

    /**
     * 深拷贝
     * @param obj 拷贝对象
     * @return
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCloneByJson(T obj) {
        return (T) new JsonObject(JsonObject.mapFrom(obj).toString()).mapTo(obj.getClass());
    }
}
