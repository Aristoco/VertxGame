package com.aristoco.core.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.Map;

/**
 * 适配vertx json和jackson
 * @author Administrator
 */
public class JsonObjectDeserializer extends JsonDeserializer<JsonObject> {
    @Override
    public JsonObject deserialize(JsonParser p, DeserializationContext context) throws IOException, JacksonException {
        Map<String, Object> map = p.readValueAs(new TypeReference<Map<String, Object>>() {});
        return new JsonObject(map);
    }
}
