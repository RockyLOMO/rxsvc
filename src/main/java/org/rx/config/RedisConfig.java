package org.rx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class RedisConfig {
    private String redisUrl;
    private String storeUrl;
    private List<String> limiterWhiteList;
}
