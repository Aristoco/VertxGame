package com.aristoco.core.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author chenguowei
 * @date 2024/6/24
 * @description jackson自定义配置
 **/
@FunctionalInterface
public interface GameJacksonConfig {

    /**
     * 定义jackson的配置
     * @param objectMapper
     * @param module
     */
    void config(ObjectMapper objectMapper, com.fasterxml.jackson.databind.Module module);

}
