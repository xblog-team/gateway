package com.xblog.gateway.filter;

import com.xblog.gateway.config.RedisConfig;
import com.xblog.gateway.exception.AccessTokenBlacklistedException;
import com.xblog.gateway.exception.AccessTokenNotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonWebTokenFilter extends AbstractGatewayFilterFactory<JsonWebTokenFilter.Config> {
    private static final String AUTHORIZATION_KEY = "Authorization";
    private static final String TOKEN_TYPE = "Bearer ";
    private static final String USER_ID_KEY = "userId";

    @Value("${filter.jwt.excludePath}")
    public String[] excludePaths;

    private final JwtParser jwtParser;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Config newConfig() {
        return new Config();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (shouldNotFilter(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            String accessToken = obtainToken(exchange.getRequest());

            try {
                Claims claims = jwtParser
                        .parseClaimsJwt(accessToken)
                        .getBody();

                if (redisTemplate.opsForValue().get(accessToken) != null) { // black list에 access token이 존재하는지 확인.
                    throw new AccessTokenBlacklistedException();
                }

                exchange.getRequest().mutate()
                        .header("X-USER-ID", claims.get(USER_ID_KEY, String.class))
                        .build();

                return chain.filter(exchange);

            } catch (ExpiredJwtException expiredJwtException) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expired token");
            } catch (Exception e) {
                log.info(e.getMessage());
                log.trace(accessToken);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid token");
            }
        });
    }

    @Deprecated
    private boolean shouldNotFilter(ServerHttpRequest request) {
        return Arrays.stream(excludePaths)
                .anyMatch(p -> request.getPath().toString().startsWith(p));
    }

    private String obtainToken(ServerHttpRequest request) {
        String jwt = request.getHeaders().getFirst(AUTHORIZATION_KEY);

        if (!StringUtils.hasText(jwt) || !jwt.startsWith(TOKEN_TYPE)) {
            throw new AccessTokenNotFoundException();
        }

        log.debug(jwt);
        return jwt.substring(TOKEN_TYPE.length());
    }

    public static class Config {
    }
}
