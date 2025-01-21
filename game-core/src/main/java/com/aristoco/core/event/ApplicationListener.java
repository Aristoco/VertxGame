/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aristoco.core.event;

/**
 * 事件监听接口
 *
 * @author Administrator
 */
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> {

    /**
     * 执行事件
     *
     * @param event the event to respond to
     */
    void onApplicationEvent(E event);

    /**
     * 是否独立监听器
     * <p> 事件处理慢时，开启可能会提升速度
     *
     */
    default boolean isAlone() {
        return false;
    }

    /**
     * 是否只消费本地事件
     * <p> 开启后只会消费当前vertx中的事件
     *
     */
    default boolean isLocal() {
        return false;
    }

    /**
     * 使用mvel表达式，判断当前事件是否能够触发
     * @see #onApplicationEvent(ApplicationEvent) 事件在方法的参数列表中的名字
     *
     */
    default String condition() {
        return "";
    }

    /**
     * 支持的事件源
     * 默认匹配所有的事件源,添加了事件源就只会监听指定事件源的
     * 所有Verticle未设置自定义事件源时，都是CommonEventSource
     * @see com.aristoco.core.bean.CommonEventSource
     */
    default Class<?>[] eventSources(){
        return new Class<?>[]{};
    }

}
