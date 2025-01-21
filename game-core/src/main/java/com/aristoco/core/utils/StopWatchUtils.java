package com.aristoco.core.utils;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.IdUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chenguowei
 * @date 2024/8/2
 * @description 计时工具类
 **/
public class StopWatchUtils {

    /**
     * 计时工具缓存
     * k:唯一id v:即时工具对象
     */
    private static final Map<String, StopWatch> stopWatchMap = new ConcurrentHashMap<>();

    /**
     * 创建一个计时器
     *
     * @return
     */
    public static StopWatch createStopWatch() {
        StopWatch stopWatch = StopWatch.create(IdUtil.fastSimpleUUID());
        stopWatchMap.put(stopWatch.getId(), stopWatch);
        return stopWatch;
    }

    /**
     * 获取指定的计时器
     * @param id 计时器id
     * @return
     */
    public static StopWatch getStopWatch(String id) {
        return stopWatchMap.get(id);
    }

    /**
     * 移除指定的计时器
     * @param id 计时器id
     */
    public static void removeStopWatch(String id) {
        stopWatchMap.remove(id);
    }
}
