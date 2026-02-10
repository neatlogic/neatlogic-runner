package com.neatlogic.autoexecrunner.api.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.dto.ApiVo;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentFactory;
import com.neatlogic.autoexecrunner.restful.core.publicapi.PublicApiComponentFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 多租户 API 限流器（按 Token 限流）
 * 特性：
 * - 每个 token 独立限流（支持不同 QPS）
 * - 自动缓存 RateLimiter，1 小时未使用自动回收
 * - 最多缓存 10,000 个活跃 token（防 OOM）
 * - 非阻塞 tryAcquire，超限立即返回 false
 */
@SuppressWarnings("UnstableApiUsage")
public final class TokenRateLimiter {

    // 缓存配置：最多 10,000 个 token，1 小时无访问自动移除
    private static final Cache<String, RateLimiter> LIMITER_CACHE = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    // QPS 提供函数：可根据 token 动态返回配额（默认 500）
    private static Function<String, Integer> qpsProvider = TokenRateLimiter::defaultQpsProvider;

    private TokenRateLimiter() {
        // 工具类，禁止实例化
    }

    /**
     * 尝试获取令牌（非阻塞）
     *
     * @param token API Token
     * @return true 表示允许请求，false 表示被限流
     */
    public static boolean tryAcquire(String token) {
        if (token == null || token.isEmpty()) {
            return false; // 无效 token 直接拒绝
        }

        RateLimiter limiter = LIMITER_CACHE.get(token, TokenRateLimiter::createLimiter);
        return limiter != null && limiter.tryAcquire();
    }

    /**
     * 动态设置 QPS 提供函数（例如从数据库或配置中心加载）
     */
    public static void setQpsProvider(Function<String, Integer> provider) {
        if (provider != null) {
            TokenRateLimiter.qpsProvider = provider;
        }
    }

    /**
     * 手动清除某个 token 的限流器（例如用户注销）
     */
    public static void invalidate(String token) {
        if (token != null) {
            LIMITER_CACHE.invalidate(token);
        }
    }

    /**
     * 清空所有缓存（谨慎使用，如配置重载）
     */
    public static void invalidateAll() {
        LIMITER_CACHE.invalidateAll();
    }

    // ———————— 内部方法 ————————

    private static RateLimiter createLimiter(String token) {
        Integer qps = qpsProvider.apply(token);
        if (qps == null || qps <= 0) {
            qps = 500; // 安全兜底
        }
        return RateLimiter.create(qps);
    }

    /**
     * 默认 QPS 策略（可被覆盖）
     */
    private static Integer defaultQpsProvider(String token) {
        ApiVo apiVo = PrivateApiComponentFactory.getApiByToken(token);
        if (apiVo == null) {
            apiVo = PublicApiComponentFactory.getApiByToken(token);
        }
        if (apiVo != null && apiVo.getQpsLimitConfigKey() != null) {
            String value = Config.getConfigProperty(apiVo.getQpsLimitConfigKey(), null);
            if (value != null) {
                return Integer.parseInt(value);
            }
        }
        return 500; // 默认
    }
}