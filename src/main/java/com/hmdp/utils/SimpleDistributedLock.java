package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 工具类：
 * 简单的可以应用于 分布式场景下 的 分布式锁的实现<br/>
 * 实现了 `过期时间预防死锁`、 `释放锁的原子性`、`防止 锁误删`<br/>
 * 不具备可重入、重试、存活时间续签  的 以及不满足`主从一致性`的分布式锁实现
 * 主从一致性 如果Redis提供了主从集群，当我们向集群写数据时，主机需要异步的将数据同步给从机，而万一在同步过去之前，主机宕机了，就会出现死锁问题。
 */
public class SimpleDistributedLock {
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleDistributedLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    /**
     * 释放锁的lua脚本
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    private static final String UNLOCK_SCRIPT_STR = "if (redis.call('get', KEYS[1]) == ARGV[1]) then" +
            "    return redis.call('del',KEYS[1]);" +
            "end;" +
            "return 0;";

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.Lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * 上锁
     * 以当前线程的线程名称作为锁
     *
     * @param timeoutSec
     * @return
     */
    public boolean tryLock(long timeoutSec) {
        //获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁
     */
    public void unlock() {
        //调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
}