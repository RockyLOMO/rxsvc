package org.rx.config;

import lombok.Data;
import org.rx.spring.SpringContext;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MiddlewareConfig {
    private String redisUrl;
    private boolean redisLocalFallback;
    private String storeUrl;
    private int limiterPermits = 12;
    private String limiterWhiteList;

    public String[] getLimiterWhiteList() {
        return SpringContext.fromYamlArray(limiterWhiteList);
    }

    private String crawlerEndpoint;
    private String fiddlerEndpoint;

    private String smtpPwd;
    private String smtpTo;
}
