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

import com.aristoco.core.utils.TimeUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 事件基类:用于事件有多个参数
 * @author Administrator
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class ApplicationEvent implements Serializable {

	@Serial
	private static final long serialVersionUID = 7099057708183571937L;

	/**
	 * 事件发生源
	 */
	private String source;

	/**
	 * 事件发生时间戳
	 */
	private final long timestamp;

	/**
	 * 创建一个事件
	 */
	public ApplicationEvent() {
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * 创建一个事件
	 * @param time 自定义事件时间
	 */
	public ApplicationEvent(LocalDateTime time) {
		this.timestamp = TimeUtils.toEpochMilli(time);
	}

	/**
	 * 创建一个事件
	 * @param time 自定义事件时间
	 */
	public ApplicationEvent(long time) {
		this.timestamp = time;
	}

}
