package com.aristoco.core.event;

import cn.hutool.core.lang.Assert;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author chenguowei
 * @date 2024/7/9
 * @description 单参数事件载体
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString(callSuper = true)
public class PayloadApplicationEvent<T> extends ApplicationEvent {

    /**
     * 事件信息
     */
    private T payload;


    public PayloadApplicationEvent(T payload) {
        Assert.notNull(payload, "Payload must not be null");
        this.payload = payload;
    }

}
