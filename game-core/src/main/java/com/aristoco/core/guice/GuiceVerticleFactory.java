/*
 * The MIT License (MIT)
 * Copyright © 2016 Englishtown <opensource@englishtown.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.aristoco.core.guice;

import com.aristoco.core.GameApplicationContext;
import com.aristoco.core.GameApplicationContextFactory;
import com.aristoco.core.exception.BaseVerticleNotFoundException;
import com.aristoco.core.vertx.verticle.BaseVerticle;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.JavaVerticleFactory;
import io.vertx.core.spi.VerticleFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 创建verticle时同时创建guice来进行依赖注入
 * 类似{@link JavaVerticleFactory}
 *
 * @author Administrator
 */
@Slf4j
public class GuiceVerticleFactory implements VerticleFactory {

    /**
     * 工厂前缀
     */
    public static final String PREFIX = "java-guice";

    /**
     * 当前vertx实例
     */
    private Vertx vertx;

    /**
     * 工厂前缀
     * xxxx:xxxxVerticle
     * 定义当前vertcile工厂的前缀,默认会根据":"前找对应的工厂
     */
    @Override
    public String prefix() {
        return PREFIX;
    }

    /**
     * Initialise the factory
     *
     * @param vertx The Vert.x instance
     */
    @Override
    public void init(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * 创建verticle
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void createVerticle(String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
        verticleName = VerticleFactory.removePrefix(verticleName);
        Class<? extends BaseVerticle> clazz;
        try {
            clazz = (Class<? extends BaseVerticle>) classLoader.loadClass(verticleName);
        } catch (ClassNotFoundException e) {
            promise.fail(e);
            return;
        }
        //执行创建
        promise.complete(() -> {
            //需要每次获取都获取一个新的上下文
            GameApplicationContext context = GameApplicationContextFactory.createGameApplicationContext(clazz);
            //构建guiceModule
            List<Module> modules = new ArrayList<>();
            modules.add(new GuiceVertxModule(vertx, context));
            //判断是否有额外的module
            if (!context.getExtraGuiceModule().isEmpty()) {
                modules.addAll(context.getExtraGuiceModule());
            }
            //构建guice依赖
            Injector injector = Guice.createInjector(modules);
            context.setInjector(injector);
            //获取引导verticle的实例
            BaseVerticle baseVerticle;
            try {
                baseVerticle = context.getInjector().getInstance(clazz);
            } catch (Exception e) {
                throw new BaseVerticleNotFoundException(e);
            }
            if (baseVerticle == null) {
                throw new BaseVerticleNotFoundException();
            }
            return baseVerticle;
        });
    }

    /**
     * 获取guice依赖注入的Verticle名
     *
     * @param verticleClass Verticle类
     * @return
     */
    public static String getGuiceVerticleName(Class<? extends BaseVerticle> verticleClass) {
        return PREFIX + ":" + verticleClass.getName();
    }
}
