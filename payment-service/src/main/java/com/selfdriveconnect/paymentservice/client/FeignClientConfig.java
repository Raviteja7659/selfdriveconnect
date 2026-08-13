package com.selfdriveconnect.paymentservice.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Without an API Gateway, each service has to forward auth context
 * itself when it calls another service on a user's behalf. This
 * interceptor copies the Authorization header from the incoming
 * request (the renter's own JWT) onto any outgoing Feign call, so
 * booking-service sees the same identity that called payment-service.
 *
 * Once an API Gateway is added, this pattern typically moves there
 * instead — the gateway validates the JWT once and either forwards
 * it or issues a short-lived internal token to downstream services.
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader("Authorization");
                if (authHeader != null) {
                    template.header("Authorization", authHeader);
                }
            }
        };
    }
}
