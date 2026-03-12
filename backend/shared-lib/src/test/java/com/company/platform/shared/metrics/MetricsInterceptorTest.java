package com.company.platform.shared.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MetricsInterceptor.class, SimpleMeterRegistry.class})
class MetricsInterceptorTest {
    
    @Autowired
    private MetricsInterceptor interceptor;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @BeforeEach
    void setUp() {
        meterRegistry.clear();
    }
    
    @Test
    void shouldRecordRequestMetrics() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/users");
        request.setMethod("GET");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        
        interceptor.preHandle(request, response, new Object());
        Thread.sleep(10);
        interceptor.afterCompletion(request, response, new Object(), null);
        
        var timer = meterRegistry.find("http.server.requests")
            .tag("method", "GET")
            .tag("uri", "/api/users")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer();
        
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }
    
    @Test
    void shouldRecordErrorMetrics() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/users/999");
        request.setMethod("GET");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        
        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);
        
        var counter = meterRegistry.find("http.server.errors")
            .tag("method", "GET")
            .tag("uri", "/api/users/999")
            .tag("status", "404")
            .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }
    
    @Test
    void shouldRecordServerErrorMetrics() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal");
        request.setMethod("POST");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        
        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);
        
        var timer = meterRegistry.find("http.server.requests")
            .tag("outcome", "SERVER_ERROR")
            .timer();
        
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        
        var counter = meterRegistry.find("http.server.errors")
            .tag("status", "500")
            .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }
    
    @Test
    void shouldNotRecordErrorMetricsForSuccessfulRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/health");
        request.setMethod("GET");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        
        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);
        
        var counter = meterRegistry.find("http.server.errors").counter();
        assertThat(counter).isNull();
    }
}
