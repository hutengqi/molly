package cn.molly.mq.core;

import cn.molly.mq.core.exception.MqSerializationException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 默认 Jackson 实现，简单对象直接 UTF-8 字节；String 特例走直接字节避免双引号包裹
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
public class JacksonMessageConverter implements MessageConverter {

    private final ObjectMapper objectMapper;

    public JacksonMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] toBytes(Object payload) {
        if (payload == null) {
            return new byte[0];
        }
        if (payload instanceof byte[] bytes) {
            return bytes;
        }
        if (payload instanceof String str) {
            return str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (IOException e) {
            throw new MqSerializationException("序列化消息失败", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromBytes(byte[] bytes, Class<T> type) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        if (type == byte[].class) {
            return (T) bytes;
        }
        if (type == String.class) {
            return (T) new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
        try {
            return objectMapper.readValue(bytes, type);
        } catch (IOException e) {
            throw new MqSerializationException("反序列化消息失败", e);
        }
    }

    @Override
    public String contentType() {
        return "application/json";
    }
}
