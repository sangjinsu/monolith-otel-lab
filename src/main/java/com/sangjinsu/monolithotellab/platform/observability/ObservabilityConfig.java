package com.sangjinsu.monolithotellab.platform.observability;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Turns the {@code @Observed} annotation into real telemetry.
 *
 * How it works (docs/observability-deep-dive.md §2):
 * ObservedAspect (requires spring-boot-starter-aop) intercepts {@code @Observed}
 * methods and wraps each call in a Micrometer Observation. Handlers registered on
 * the ObservationRegistry then fan out — the tracing handler opens a span (exported
 * via OTLP to the Collector, then Tempo) and the metrics handler records a timer.
 *
 * This bean is the ONLY observability wiring needed in code; exporters, sampler and
 * resource attributes are configured in application.yml (management.*). Keeping it in
 * platform.observability isolates plumbing from business packages (AGENTS.md rule 7).
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
