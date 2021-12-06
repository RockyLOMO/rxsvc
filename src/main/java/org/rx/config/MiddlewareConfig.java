package org.rx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "middleware")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MiddlewareConfig {
    private String crawlerEndpoint;
}
