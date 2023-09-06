package com.dqv5.autogennum.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * https://mp.weixin.qq.com/s/qJK61ew0kCExvXrqb7-RSg
 */
public class RedisTool {

    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "EX";

    private static final Long RELEASE_SUCCESS = 1L;


    /**
     * 尝试获取分布式锁
     *
     * @param jedis      Redis客户端
     * @param lockKey    锁
     * @param requestId  请求标识
     * @param expireTime 超期时间
     * @return 是否获取成功
     */

    public static boolean tryGetDistributedLock(Jedis jedis, String lockKey, String requestId, int expireTime) {
//        String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
        String result = jedis.set(lockKey, requestId, SetParams.setParams().nx().ex(expireTime));
        return LOCK_SUCCESS.equals(result);
    }

    /**
     * 尝试获取分布式锁
     *
     * @param stringRedisTemplate Redis客户端
     * @param lockKey             锁
     * @param requestId           请求标识
     * @param expireTime          超期时间
     * @return 是否获取成功
     */

    public static boolean tryGetDistributedLock(StringRedisTemplate stringRedisTemplate, String lockKey, String requestId, int expireTime) {
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, requestId, expireTime, TimeUnit.SECONDS);
        return result != null && result;
    }


    /**
     * 释放分布式锁
     *
     * @param jedis     Redis客户端
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */

    public static boolean releaseDistributedLock(Jedis jedis, String lockKey, String requestId) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
        return RELEASE_SUCCESS.equals(result);
    }

    /**
     * 释放分布式锁
     *
     * @param stringRedisTemplate Redis客户端
     * @param lockKey             锁
     * @param requestId           请求标识
     * @return 是否释放成功
     */

    public static boolean releaseDistributedLock(StringRedisTemplate stringRedisTemplate, String lockKey, String requestId) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), requestId);
        return RELEASE_SUCCESS.equals(result);
    }


}
