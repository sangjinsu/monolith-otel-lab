package com.sangjinsu.monolithotellab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Layer 4: @SpringBootTest smoke test — verifies the full ApplicationContext wires up,
 * including the Micrometer Tracing {@code Tracer}, {@code MeterRegistry},
 * {@code ObservedAspect}, JPA, and the web layer.
 *
 * Overrides the datasource to embedded H2 and points the OTLP exporter at a dummy
 * endpoint (the exporter runs in batch mode, so startup does not depend on it).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "management.otlp.tracing.endpoint=http://localhost:14318/v1/traces"
})
class MonolithOtelLabApplicationTests {

    @Test
    void contextLoads() {
    }
}
