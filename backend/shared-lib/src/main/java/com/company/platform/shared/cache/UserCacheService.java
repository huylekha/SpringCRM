package com.company.platform.shared.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnBean(StringRedisTemplate.class)
public class UserCacheService {

  private static final String KEY_PREFIX = "user:";
  private static final Duration TTL = Duration.ofMinutes(30);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public UserCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public Optional<CachedUserInfo> getUser(UUID userId) {
    String key = KEY_PREFIX + userId;
    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json == null) {
        log.debug("Cache miss for user: {}", userId);
        return Optional.empty();
      }
      CachedUserInfo info = objectMapper.readValue(json, CachedUserInfo.class);
      log.debug("Cache hit for user: {}", userId);
      return Optional.of(info);
    } catch (Exception e) {
      log.warn("Failed to read user from cache: userId={}, error={}", userId, e.getMessage());
      return Optional.empty();
    }
  }

  public void putUser(CachedUserInfo userInfo) {
    String key = KEY_PREFIX + userInfo.userId();
    try {
      String json = objectMapper.writeValueAsString(userInfo);
      redisTemplate.opsForValue().set(key, json, TTL);
      log.debug("Cached user: {}", userInfo.userId());
    } catch (JsonProcessingException e) {
      log.warn("Failed to cache user: userId={}, error={}", userInfo.userId(), e.getMessage());
    }
  }

  public void evictUser(UUID userId) {
    String key = KEY_PREFIX + userId;
    redisTemplate.delete(key);
    log.debug("Evicted user from cache: {}", userId);
  }
}
