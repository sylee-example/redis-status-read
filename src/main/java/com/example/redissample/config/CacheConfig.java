package com.example.redissample.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

  // 캐시 put/evict 를 DB 커밋 이후로 미룸 (롤백되면 캐시도 안 건드림)
  @Bean
  public RedisCacheManagerBuilderCustomizer transactionAwareCache() {
    return RedisCacheManagerBuilder::transactionAware;
  }

  // Redis 장애 시 캐시 에러는 로그만 남기고 무시 → 조회는 DB 로 처리, 상태 변경도 그대로 성공
  // (그 사이 못 지운 캐시는 TTL 이 지나면 사라짐)
  @Override
  public CacheErrorHandler errorHandler() {
    return new LoggingCacheErrorHandler();
  }
}
