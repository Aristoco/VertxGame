package com.aristoco.core;

import com.aristoco.core.exception.NotInVerticleContextException;
import com.aristoco.core.exception.VerticleInitNotFinishException;
import com.aristoco.core.vertx.verticle.BaseVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chenguowei
 * @date 2024/6/14
 * @description 应用上下文工厂
 **/
public class GameApplicationContextFactory {

    /**
     * 保存Verticle的上下文
     * k: verticle类型 v: k1: 上下文名(DisplayName) v1:上下文对象
     */
    public static final Map<Class<? extends BaseVerticle>, Map<String, GameApplicationContext>>
            GAME_APPLICATION_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * 创建上下文
     * <p> 非引导Verticle每次调用都会创建新的上下文对象
     * <p> 同类型的Verticle上下文对象都是相同的深拷贝
     *
     * @return Verticle上下文
     */
    @SuppressWarnings("unchecked")
    public synchronized static <T extends GameApplicationContext> T createGameApplicationContext(Class<? extends BaseVerticle> clazz) {
        BootstrapApplicationContext bootstrapApplicationContext = getBootstrapApplicationContext();
        if (BootstrapVerticle.class.isAssignableFrom(clazz)) {
            return (T) bootstrapApplicationContext;
        }
        Map<String, GameApplicationContext> applicationContextMap = GAME_APPLICATION_CONTEXT_MAP.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        //需要特殊处理,当不为null时后续的深度复制已存在的
        GameApplicationContext gameApplicationContext = applicationContextMap.values()
                .stream()
                .findFirst()
                .orElse(bootstrapApplicationContext)
                .copy();
        applicationContextMap.put(gameApplicationContext.getDisplayName(), gameApplicationContext);
        return (T) gameApplicationContext;
    }

    /**
     * 获取上下文
     *
     * @param clazz       Verticle类
     * @param displayName 上下文名
     * @param <T>
     * @return 返回对应的上下文
     */
    @SuppressWarnings("unchecked")
    public static <T extends GameApplicationContext> T getGameApplicationContext(Class<? extends BaseVerticle> clazz,
                                                                                 String displayName) {
        if (BootstrapVerticle.class.isAssignableFrom(clazz)) {
            return (T) getBootstrapApplicationContext();
        }
        return (T) GAME_APPLICATION_CONTEXT_MAP.getOrDefault(clazz, new ConcurrentHashMap<>())
                .get(displayName);
    }

    /**
     * 获取引导Verticle的上下文
     *
     * @return
     */
    public static BootstrapApplicationContext getBootstrapApplicationContext() {
        return (BootstrapApplicationContext) GAME_APPLICATION_CONTEXT_MAP
                .computeIfAbsent(BootstrapVerticle.class, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(BootstrapVerticle.class.getSimpleName(), k -> new BootstrapApplicationContext());
    }

    /**
     * 应用上下文初始化完成
     */
    public static void applicationContextInitFinish() {
        for (Map<String, GameApplicationContext> contextMap : GAME_APPLICATION_CONTEXT_MAP.values()) {
            contextMap.values().forEach(gameApplicationContext -> gameApplicationContext.setInit(true));
        }
    }

    /**
     * 获取当前上下文
     *
     * @param <T>
     * @return 返回对应的上下文
     */
    @SuppressWarnings("unchecked")
    public static <T extends GameApplicationContext> T getCurrentGameApplicationContext() {
        Context context = Vertx.currentContext();
        if (context == null) {
            throw new NotInVerticleContextException();
        }
        String verticleClassName = Vertx.currentContext().get(BaseVerticle.VERTICLE_CLASS_NAME);
        String applicationContextDisplayName = Vertx.currentContext().get(BaseVerticle.VERTICLE_APPLICATION_CONTEXT_DISPLAY_NAME);
        for (Map.Entry<Class<? extends BaseVerticle>, Map<String, GameApplicationContext>> entry : GAME_APPLICATION_CONTEXT_MAP.entrySet()) {
            if (entry.getKey().getName().equals(verticleClassName)) {
                return (T) entry.getValue().get(applicationContextDisplayName);
            }
        }
        throw new VerticleInitNotFinishException();
    }

}
