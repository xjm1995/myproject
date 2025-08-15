package com.zj.xjm.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedissonClient redissonClient;

    /**
     * 获取接口级别令牌（简化版）
     *
     * @param api  接口（如"/api/lottery/draw"）
     * @param rate     允许的请求速率
     * @param interval 时间间隔
     * @param unit     时间单位
     * @return true-获取成功，false-被限流
     */
    public boolean tryAcquireApiToken(String api, long rate, long interval, RateIntervalUnit unit) {
        String key = "api_rate_limit:" + api;
        RRateLimiter limiter = getInitializedRateLimiter(key, rate, interval, unit);
        return limiter.tryAcquire();
    }

    /**
     * 获取接口级别令牌（带超时等待）
     *
     * @param api     接口
     * @param rate        请求速率
     * @param interval    时间间隔
     * @param unit        时间单位
     * @param timeout     等待超时时间
     * @param timeoutUnit 超时时间单位
     * @return true-获取成功，false-超时或被限流
     */
    public boolean tryAcquireApiToken(String api, long rate, long interval,
                                      RateIntervalUnit unit, long timeout, TimeUnit timeoutUnit) {
        String key = "api_rate_limit:" + api;
        RRateLimiter limiter = getInitializedRateLimiter(key, rate, interval, unit);
        return limiter.tryAcquire(1, timeout, timeoutUnit);
    }

    /**
     * 获取会员级别令牌（简化版）
     *
     * @param memberId 会员ID
     * @param rate     允许的请求速率
     * @param interval 时间间隔
     * @param unit     时间单位
     * @return true-获取成功，false-被限流
     */
    public boolean tryAcquireMemberToken(String memberId, long rate, long interval, RateIntervalUnit unit) {
        String key = "member_rate_limit:" + memberId;
        RRateLimiter limiter = getInitializedRateLimiter(key, rate, interval, unit);
        return limiter.tryAcquire();
    }

    /**
     * 批量获取会员令牌（适合批量操作场景）
     *
     * @param memberId 会员ID
     * @param permits  需要的令牌数量
     * @param rate     请求速率
     * @param interval 时间间隔
     * @param unit     时间单位
     * @return true-获取成功，false-被限流
     */
    public boolean tryAcquireMemberTokens(String memberId, int permits,
                                          long rate, long interval, RateIntervalUnit unit) {
        String key = "member_rate_limit:" + memberId;
        RRateLimiter limiter = getInitializedRateLimiter(key, rate, interval, unit);
        return limiter.tryAcquire(permits);
    }

    /**
     * Key级别的初始化锁映射表
     * Key: 限流器的Redis键
     * Value: 该键对应的同步锁对象
     * 使用ConcurrentHashMap保证线程安全
     */
    private final ConcurrentHashMap<String, Object> initLocks = new ConcurrentHashMap<>();

    /**
     * 获取已初始化的限流器
     *
     * @param key      限流器的唯一标识（如"api:lottery:1"）
     * @param rate     允许的请求速率（如1000次/秒）
     * @param interval 时间间隔（如1秒）
     * @param unit     时间单位（秒/分钟等）
     * @return 已配置好的限流器实例
     */
    public RRateLimiter getInitializedRateLimiter(String key, long rate,
                                                  long interval, RateIntervalUnit unit) {
        // 从Redisson获取限流器实例（此时可能未初始化）
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // 快速检查：如果限流器已初始化，直接返回（避免锁竞争）
        if (isInitialized(rateLimiter)) {
            return rateLimiter;
        }

        // 需要初始化：进入双重检查锁定流程
        return initializeRateLimiter(key, rateLimiter, rate, interval, unit);
    }

    /**
     * 检查限流器是否已初始化
     *
     * @param rateLimiter 限流器实例
     * @return true-已初始化，false-未初始化
     */
    private boolean isInitialized(RRateLimiter rateLimiter) {
        try {
            // 获取限流器配置，检查rate是否大于0
            // 注意：此操作会访问Redis，但Redisson有本地缓存优化
            return rateLimiter.getConfig().getRate() > 0;
        } catch (Exception e) {
            // 如果获取配置失败（如网络问题），保守返回未初始化
            // 让后续流程处理可能的异常
            return false;
        }
    }

    /**
     * 初始化限流器（使用双重检查锁模式）
     *
     * @param key         限流器键
     * @param rateLimiter 限流器实例
     * @param rate        请求速率
     * @param interval    时间间隔
     * @param unit        时间单位
     * @return 已初始化的限流器
     */
    private RRateLimiter initializeRateLimiter(String key, RRateLimiter rateLimiter,
                                               long rate, long interval, RateIntervalUnit unit) {
        // 获取或创建该Key对应的锁对象
        // computeIfAbsent保证同一Key返回同一个锁对象
        Object lock = initLocks.computeIfAbsent(key, k -> new Object());

        // 同步块开始（只锁定当前Key的初始化过程）
        synchronized (lock) {
            // 双重检查：其他线程可能已经完成初始化
            if (isInitialized(rateLimiter)) {
                return rateLimiter;
            }

            // 实际初始化操作（Redis执行Lua脚本）
            // trySetRate是幂等的，重复调用无副作用
            rateLimiter.trySetRate(
                    RateType.OVERALL,  // 全局限流模式
                    rate,              // 允许的请求速率
                    interval,          // 时间间隔
                    unit               // 时间单位
            );

            return rateLimiter;
        }
        // 同步块结束
    }
}