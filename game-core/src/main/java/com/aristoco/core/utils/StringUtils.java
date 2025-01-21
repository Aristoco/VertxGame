package com.aristoco.core.utils;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;

/**
 * @author chenguowei
 * @date 2024/6/18
 * @description
 **/
public class StringUtils extends StrUtil {

    /**
     * 转换为首字母小写的驼峰格式
     * @param className
     * @return
     */
    public static String toLowerCaseCamel(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        // 将类名的首字母转换为小写
        return className.substring(0, 1).toLowerCase(Locale.US)
                + (className.length() > 1 ? className.substring(1) : "");
    }

}
