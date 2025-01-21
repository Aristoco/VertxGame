package com.aristoco.core.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 上下文事件抽象类
 * @author Administrator
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ApplicationContextEvent extends ApplicationEvent {


}