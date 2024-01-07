package com.monitor.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<Object, Object> objectTemplate;

    @Autowired
    private RedisTemplate<String, String> stringTemplate;

    @Autowired
    private RedisTemplate<String, Boolean> booleanTemplate;

    public String getStringValueOrDefault(String key, String defaultValue) {
        String value = stringTemplate.opsForValue().get(key);
        return (value == null ? defaultValue : value);
    }

    public void saveStringValue(String key, String value, long timeout, TimeUnit unit) {
        if (timeout == 0) {
            stringTemplate.opsForValue().set(key, value);
        } else {
            stringTemplate.opsForValue().set(key, value, timeout, unit);
        }
    }

    public void saveObjectValue(String key, Object object) {
        objectTemplate.opsForValue().set(key, object);
    }

    public Object getObjectValue(String key) {
        return objectTemplate.opsForValue().get(key);
    }

    public void saveBooleanValue(String key, Boolean value, long timeout, TimeUnit unit) {
        booleanTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Boolean getBooleanValue(String key) {
        Boolean value = booleanTemplate.opsForValue().get(key);
        return value != null && value;
    }

    public void deleteBooleanKey(String key) {
        this.booleanTemplate.delete(key);
    }
}
