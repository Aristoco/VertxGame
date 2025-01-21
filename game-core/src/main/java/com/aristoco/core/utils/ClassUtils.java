package com.aristoco.core.utils;

import cn.hutool.core.util.ClassUtil;
import com.aristoco.core.exception.UnsupportedTypeException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author chenguowei
 * @date 2024/7/5
 * @description 类工具
 **/
public class ClassUtils extends ClassUtil {

    /**
     * 辅助方法检查是否为基本类型或String
     *
     * @param clazz
     * @return
     */
    public static boolean isPrimitiveOrString(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.equals(String.class)
                || clazz.equals(Boolean.class)
                || clazz.equals(Integer.class)
                || clazz.equals(Character.class)
                || clazz.equals(Long.class)
                || clazz.equals(Double.class)
                || clazz.equals(Float.class)
                || clazz.equals(Byte.class)
                || clazz.equals(Short.class);
    }

    /**
     * 转换string为不同类型
     *
     * @param s
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T convertStringToType(String s, Class<T> clazz) {
        if (s == null || clazz == null) {
            throw new IllegalArgumentException("输入的参数为null");
        }
        if (clazz.equals(String.class)) {
            return clazz.cast(s);
        }
        if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            return clazz.cast(Integer.parseInt(s));
        } else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            return clazz.cast(Boolean.valueOf(s));
        } else if (clazz.equals(Byte.class) || clazz.equals(byte.class)) {
            return clazz.cast(Byte.parseByte(s));
        } else if (clazz.equals(Short.class) || clazz.equals(short.class)) {
            return clazz.cast(Short.parseShort(s));
        } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            return clazz.cast(Long.parseLong(s));
        } else if (clazz.equals(Float.class) || clazz.equals(float.class)) {
            return clazz.cast(Float.parseFloat(s));
        } else if (clazz.equals(Double.class) || clazz.equals(double.class)) {
            return clazz.cast(Double.parseDouble(s));
        } else if (clazz.equals(Character.class) || clazz.equals(char.class)) {
            return clazz.cast(!s.isEmpty() ? s.charAt(0) : '\0');
        } else if (clazz.equals(BigInteger.class)) {
            return clazz.cast(new BigInteger(s));
        } else if (clazz.equals(BigDecimal.class)) {
            return clazz.cast(new BigDecimal(s));
        } else {
            throw new UnsupportedTypeException("不支持的类型." +
                    " 当前只支持基本类和其包装类以及String,BigInteger,BigDecimal进行转换.");
        }
    }

    /**
     * 获取类名的首字母小写的驼峰命名
     *
     * @param clazz
     * @return
     */
    public static String getClassLowerCaseCamel(Class<?> clazz) {
        return StringUtils.toLowerCaseCamel(clazz.getSimpleName());
    }
}
