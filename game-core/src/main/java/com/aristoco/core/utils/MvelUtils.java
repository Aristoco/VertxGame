package com.aristoco.core.utils;

import com.aristoco.core.el.JsonArrayPropertyHandler;
import com.aristoco.core.el.JsonObjectPropertyHandler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.PropertyHandlerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chenguowei
 * @date 2024/7/24
 * @description mvel工具类
 * 支持的格式
 * <p>	1.#{my.app.name}
 * <p>	2.${my.app.name}
 * <p>	3.${my.app.name:1} 默认值支持,使用了默认值就不能再使用算术和逻辑等计算
 * <p>	4.#{${my.app.name:1} > 0 ? 1:0 } 默认值和计算混合使用
 * <p>	5.my.app.name 直接作为字符串赋值不经过表达式解析
 **/
public class MvelUtils {

    /**
     * 匹配插值表达式
     * ${xxxx}
     */
    public static Pattern valuePattern = Pattern.compile("\\$\\{[^}]*}");

    /**
     * mvel的上下文
     */
    private static final ParserContext parserContext = new ParserContext();

    static {
        //支持vertx json解析
        registerPropertyHandler(JsonObject.class, new JsonObjectPropertyHandler());
        registerPropertyHandler(JsonArray.class, new JsonArrayPropertyHandler());
    }

    /**
     * 获取mvel设置上下文
     * @return
     */
    public static ParserContext getParserContext(){
        return parserContext;
    }

    /**
     * 注册类处理器
     * @param clazz  需要处理的类
     * @param propertyHandler 对应类读取写入处理器
     */
    @SuppressWarnings("rawtypes")
    public static void registerPropertyHandler(Class clazz, PropertyHandler propertyHandler) {
        PropertyHandlerFactory.registerPropertyHandler(clazz, propertyHandler);
    }

    /**
     * 执行表达式
     *
     * @param expression 表达式
     * @param params     参数
     * @param returnType 返回类型
     * @param <T> 返回类型
     * @return null/对应的值
     */
    public static <T> T handleExpression(Serializable expression, Map<String, Object> params, Class<T> returnType) {
        return MVEL.executeExpression(expression, parserContext, params, returnType);
    }

    /**
     * 执行表达式
     *
     * @param expressionStr 表达式字符串
     * @param params        参数
     * @param returnType    返回类型
     * @param <T> 返回类型
     * @return null/对应的值
     */
    public static <T> T handleExpression(String expressionStr, Map<String, Object> params, Class<T> returnType) {
        return MVEL.executeExpression(compileExpression(expressionStr), parserContext, params, returnType);
    }

    /**
     * 预编译表达式
     *
     * @param expressionStr 表达式字符串
     * @return 编译后表达式
     */
    public static Serializable compileExpression(String expressionStr) {
        return MVEL.compileExpression(formatExpressionStr(expressionStr), parserContext);
    }

    /**
     * 格式化表达式
     *
     * @param expressionStr 表达式字符串
     * @return 格式化后的表达式字符串
     */
    public static String formatExpressionStr(String expressionStr) {
        //必须要是这两种开头的才进行解析
        if (!expressionStr.startsWith("#{") && !expressionStr.startsWith("${")) {
            //非解析直接按照字串解析返回
            return "'" + expressionStr + "'";
        }
        //去掉外部包装
        if (expressionStr.contains("#{")) {
            expressionStr = expressionStr.replace("#{", "");
            expressionStr = expressionStr.substring(0, expressionStr.length() - 1);
        }
        Matcher matcher = valuePattern.matcher(expressionStr);
        while (matcher.find()) {
            String checkStr = matcher.group();
            //表达式替换
            String changeStr = checkStr.replace("${", "");
            changeStr = changeStr.substring(0, changeStr.length() - 1);
            //判断是否是默认值表达式
            if (checkStr.contains(":") && !checkStr.contains("?")) {
                //默认值表达式替换为判空表达式
                String[] split = changeStr.split(":");
                changeStr = "((isdef " + split[0] + ") ? " + split[0] + " : '" + split[1] + "')";
            } else {
                changeStr = "(" + changeStr + ")";
            }
            expressionStr = expressionStr.replace(checkStr, changeStr);
        }
        return expressionStr;
    }

}
