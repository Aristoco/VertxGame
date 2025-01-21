package com.aristoco.core.jackson.databind;

import com.aristoco.core.event.PayloadApplicationEvent;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * @author chenguowei
 * @date 2024/7/22
 * @description
 **/
public class PayloadApplicationEventDeserializer  extends JsonDeserializer<PayloadApplicationEvent<?>> {

    /**
     * 事件包装反序列化
     * @param p Parsed used for reading JSON content
     * @param ctxt Context that can be used to access information about
     *   this deserialization activity.
     *
     * @return
     * @throws IOException
     * @throws JacksonException
     */
    @Override
    public PayloadApplicationEvent<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return null;
    }
}
