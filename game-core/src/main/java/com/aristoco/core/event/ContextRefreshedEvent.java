package com.aristoco.core.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 容器刷新完成事件
 * @author Administrator
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ContextRefreshedEvent extends ApplicationContextEvent {


}