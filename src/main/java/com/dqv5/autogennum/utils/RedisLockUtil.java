package com.dqv5.autogennum.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
public class RedisLockUtil {

    private static final long DEFAULT_EXPIRE_UNUSED = 60000L;

    public static void lock(String lockKey) {
        Lock lock = obtainLock(lockKey);
        lock.lock();
    }

    public static boolean tryLock(String lockKey) {
        Lock lock = obtainLock(lockKey);
        return lock.tryLock();
    }

    public static boolean tryLock(String lockKey, long seconds) {
        Lock lock = obtainLock(lockKey);
        try {
            return lock.tryLock(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public static void unlock(String lockKey) {
        try {
            Lock lock = obtainLock(lockKey);
            lock.unlock();
            getRedisLockRegistry().expireUnusedOlderThan(DEFAULT_EXPIRE_UNUSED);
        } catch (Exception e) {
            log.error("分布式锁 [{}] 释放异常", lockKey, e);
        }
    }

    private static RedisLockRegistry getRedisLockRegistry() {
        return SpringUtil.getBean(RedisLockRegistry.class);
    }

    private static Lock obtainLock(String lockKey) {
        RedisLockRegistry redisLockRegistry = getRedisLockRegistry();
        return redisLockRegistry.obtain(lockKey);
    }

}
