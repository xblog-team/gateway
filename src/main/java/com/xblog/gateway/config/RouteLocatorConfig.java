package com.xblog.gateway.config;

import com.xblog.gateway.filter.JsonWebTokenFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RouteLocatorConfig {
    private final JsonWebTokenFilter jsonWebTokenFilter;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p.path("/auth/**")
                        .uri("lb://auth-service"))
                .route(p -> p.path("/chats/**")
                        .filters(f -> f)
                        .uri("lb://chat-service"))
                .route(p -> p.path("/api/posts/**")
                        .filters(f -> {
                            f.rewritePath("^/api", "");
                            return f.filter(jsonWebTokenFilter.apply(jsonWebTokenFilter.newConfig()));
                        })
                        .uri("lb://-service")) // TODO, 서비스 이름 변경.
                .build();
    }
}
