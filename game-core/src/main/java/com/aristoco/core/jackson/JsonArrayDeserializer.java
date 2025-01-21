package com.aristoco.core.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.vertx.core.json.JsonArray;

import java.io.IOException;
import java.util.List;

/**
 * 适配vertx json和jackson
 * @author Administrator
 */
public class JsonArrayDeserializer extends JsonDeserializer<JsonArray> {
    @Override
    public JsonArray deserialize(JsonParser p, DeserializationContext context) throws IOException, JacksonException {
        List<Object> list = p.readValueAs(new TypeReference<List<Object>>() {
        });
        return new JsonArray(list);
    }
}
