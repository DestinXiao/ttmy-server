package com.maple.game.osee.common;

import com.maple.database.config.redis.RedisHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Configuration
public class RedisUtil {

    private static RedisTemplate<String, String> redisTemplate = null;

    @Autowired
    public RedisUtil(RedisTemplate<String, String> redisTemplate) {
        RedisUtil.redisTemplate = redisTemplate;
    }


    /**
     * 从Redis中获取指定键对应的值，并将其转换为指定类型, 如果值为空，则设置为默认值
     * @param key 键
     * @param defaultValue 默认值
     * @return 指定键对应的值 或 默认值
     */
    @SuppressWarnings("unchecked")
    public static <T> T val(String key, T defaultValue) {
        String str = RedisHelper.get(key);
        if(str.equals("")) return defaultValue;
        if(defaultValue instanceof String)
            return (T)str;
        if(defaultValue instanceof Integer) {
            return (T)Integer.valueOf(str);
        }
        if(defaultValue instanceof Long) {
            return (T)Long.valueOf(str);
        }
        if(defaultValue instanceof Double) {
            return (T)Double.valueOf(str);
        }
        if(defaultValue instanceof Float) {
            return (T)Float.valueOf(str);
        }

        return defaultValue;
    }


    public static String get(String key) {
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        return operations.get(key);
    }

    public static Set<String> values(String key, int start, int end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    // List 操作

    public static void rightPush(String key, String... val) {
        ListOperations<String, String> ops = redisTemplate.opsForList();
        ops.rightPushAll(key, val);
    }

    public static Long size(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size == null ? 0 : size;
    }

    public static void set(String key, String val, int index) {
        redisTemplate.opsForList().set(key, index, val);
    }

    public static int get(String key, int index) {
        String s = redisTemplate.opsForList().index(key, index);
        return s == null ? 0 : Integer.valueOf(s);
    }

    public static List<String> getList(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    // ZSet 操作
    public static void zadd(String key, String value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public static Double zScore(String key, String member) {
        return redisTemplate.opsForZSet().score(key, member);
    }
}
