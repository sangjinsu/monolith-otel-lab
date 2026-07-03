package com.sangjinsu.monolithotellab.platform.observability;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the {@code @Observed} annotation so layer methods create a span + metric
 * via Micrometer Observation. The actual tracing/metrics exporters are auto-configured
 * by Spring Boot (Micrometer Tracing OTLP bridge + Actuator Prometheus registry).
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
