package com.sangjinsu.monolithotellab.payment;

import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Fake payment client. Does not call any real external API.
 * Default: sleep 50~300ms then succeed.
 * failPayment=true: sleep 100ms then throw, so a failure span can be observed.
 */
@Component
public class FakePaymentClient {

    public static final String PROVIDER = "fake";

    private final Tracer tracer;

    public FakePaymentClient(Tracer tracer) {
        this.tracer = tracer;
    }

    @Observed(name = "payment.authorize", contextualName = "PaymentClient.authorize")
    public void authorize(String orderId, boolean failPayment) {
        tagSpan("payment.provider", PROVIDER);

        if (failPayment) {
            sleep(100);
            tagSpan("payment.result", "failed");
            throw new PaymentAuthorizationException("payment authorization failed");
        }

        sleep(ThreadLocalRandom.current().nextInt(50, 301));
        tagSpan("payment.result", "success");
    }

    private void tagSpan(String key, String value) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag(key, value);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
