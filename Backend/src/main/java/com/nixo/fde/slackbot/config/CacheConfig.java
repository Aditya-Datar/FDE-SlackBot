package com.nixo.fde.slackbot.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Creates simple in-memory maps for our data
        return new ConcurrentMapCacheManager("embeddings", "classifications");
    }

    // Safety: Clear cache every hour to prevent memory leaks in a long-running app
    @Scheduled(fixedRate = 3600000)
    public void evictAllCaches() {
        cacheManager().getCacheNames()
                .forEach(cacheName -> cacheManager().getCache(cacheName).clear());
    }
}