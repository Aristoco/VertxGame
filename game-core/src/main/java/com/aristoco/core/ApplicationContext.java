package com.aristoco.core;

/**
 * @author chenguowei
 * @date 2024/7/9
 * @description
 **/
public interface ApplicationContext {

    /**
     * 获取上下文名
     * @return
     */
    String getDisplayName();

    /**
     * 返回上下文启动时间
     * @return 毫秒时间戳
     */
    long getStartupDate();

}
