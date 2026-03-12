package com.company.platform.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MetricsInterceptor implements HandlerInterceptor {
    
    private final MeterRegistry meterRegistry;
    private static final String START_TIME = "metricsStartTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String path = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();
            
            // Record request duration
            Timer.builder("http.server.requests")
                .tag("method", method)
                .tag("uri", path)
                .tag("status", String.valueOf(status))
                .tag("outcome", getOutcome(status))
                .register(meterRegistry)
                .record(duration, TimeUnit.MILLISECONDS);
            
            // Count errors
            if (status >= 400) {
                Counter.builder("http.server.errors")
                    .tag("method", method)
                    .tag("uri", path)
                    .tag("status", String.valueOf(status))
                    .register(meterRegistry)
                    .increment();
            }
        }
    }
    
    private String getOutcome(int status) {
        if (status >= 200 && status < 300) return "SUCCESS";
        if (status >= 400 && status < 500) return "CLIENT_ERROR";
        if (status >= 500) return "SERVER_ERROR";
        return "UNKNOWN";
    }
}
