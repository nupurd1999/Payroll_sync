package com.nupoor.payrollsync.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Bean
    @ConditionalOnProperty(name = "redisson.enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://" + redisHost + ":" + redisPort)
                    .setConnectTimeout(1000)
                    .setTimeout(1000)
                    .setRetryAttempts(1);
            
            RedissonClient client = Redisson.create(config);
            log.info("RedissonClient successfully connected to Redis at {}:{}", redisHost, redisPort);
            return client;
        } catch (Exception e) {
            log.warn("Redis is not running at {}:{}. Operating in standalone fallback mode without Redisson locking.", redisHost, redisPort);
            return null;
        }
    }
}
