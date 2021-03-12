package org.rx.config;

import lombok.Data;
import org.rx.spring.SpringContext;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class RedisConfig {
    private String redisUrl;
    private String storeUrl;
    private int limiterPermits = 12;
    private String limiterWhiteList;

    public String[] getLimiterWhiteList() {
        return SpringContext.fromYamlArray(limiterWhiteList);
    }
}
