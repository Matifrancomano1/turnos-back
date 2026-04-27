package com.turnos.saas.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnos.saas.dto.response.Responses.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> generalBuckets  = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets     = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    private final long generalCapacity;
    private final long authCapacity;

    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.general-capacity:100}") long generalCapacity,
            @Value("${app.rate-limit.auth-capacity:10}") long authCapacity
    ) {
        this.objectMapper    = objectMapper;
        this.generalCapacity = generalCapacity;
        this.authCapacity    = authCapacity;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        String path = request.getRequestURI();
        boolean isAuthPath = path.startsWith("/api/v1/auth");

        Bucket bucket = isAuthPath
                ? authBuckets.computeIfAbsent(ip, k -> buildBucket(authCapacity))
                : generalBuckets.computeIfAbsent(ip, k -> buildBucket(generalCapacity));

        long remaining = bucket.getAvailableTokens();

        if (bucket.tryConsume(1)) {
            response.addHeader("X-RateLimit-Remaining", String.valueOf(remaining - 1));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={}, path={}", ip, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("Retry-After", "60");
            response.addHeader("X-RateLimit-Remaining", "0");

            ErrorResponse error = ErrorResponse.of(
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Retry after 60 seconds."
            );
            objectMapper.writeValue(response.getOutputStream(), error);
        }
    }

    private Bucket buildBucket(long capacity) {
        Bandwidth bandwidth = Bandwidth.classic(
                capacity,
                Refill.greedy(capacity, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
