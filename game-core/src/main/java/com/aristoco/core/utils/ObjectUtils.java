package com.aristoco.core.utils;

import cn.hutool.core.util.ObjectUtil;
import io.vertx.codegen.annotations.Nullable;

/**
 * @author chenguowei
 * @date 2024/7/9
 * @description 对象工具类
 **/
public class ObjectUtils extends ObjectUtil {

    private static final String EMPTY_STRING = "";

    /**
     * 返回对象的整体标识的字符串表示形式
     * @param obj the object (may be {@code null})
     * @return the object's identity as String representation,
     * or an empty String if the object was {@code null}
     */
    public static String identityToString(@Nullable Object obj) {
        if (obj == null) {
            return EMPTY_STRING;
        }
        return obj.getClass().getName() + "@" + getIdentityHexString(obj);
    }

    /**
     * 返回对象标识哈希码的十六进制字符串形式。
     * @param obj the object
     * @return the object's identity code in hex notation
     */
    public static String getIdentityHexString(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }

}
