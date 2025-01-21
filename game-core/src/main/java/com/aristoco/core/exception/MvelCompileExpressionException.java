package com.aristoco.core.exception;

import com.aristoco.core.utils.StringUtils;

/**
 * @author chenguowei
 * @date 2024/7/26
 * @description
 **/
public class MvelCompileExpressionException extends RuntimeException {

    public MvelCompileExpressionException(String className, String expressionStr, Throwable cause) {
        super(StringUtils.format("当前时间监听器条件表达式预编译出问题,className:{},expressionStr:{}",
                className, expressionStr), cause);
    }
}
