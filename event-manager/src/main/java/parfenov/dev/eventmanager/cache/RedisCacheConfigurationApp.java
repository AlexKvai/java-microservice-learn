package parfenov.dev.eventmanager.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfigurationApp implements CachingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfigurationApp.class);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)))
            .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis GET failed cache={} key={}; using database", cache.getName(), key, e);
            }
            @Override public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Redis PUT failed cache={} key={}", cache.getName(), key, e);
            }
            @Override public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis EVICT failed cache={} key={}", cache.getName(), key, e);
            }
            @Override public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Redis CLEAR failed cache={}", cache.getName(), e);
            }
        };
    }
}
