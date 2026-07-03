package com.sangjinsu.monolithotellab.order;

import com.sangjinsu.monolithotellab.inventory.InventoryService;
import com.sangjinsu.monolithotellab.order.dto.CreateOrderRequest;
import com.sangjinsu.monolithotellab.order.dto.CreateOrderResponse;
import com.sangjinsu.monolithotellab.order.dto.OrderResponse;
import com.sangjinsu.monolithotellab.payment.FakePaymentClient;
import com.sangjinsu.monolithotellab.payment.PaymentAuthorizationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final FakePaymentClient paymentClient;
    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;
    private final Counter createdCounter;
    private final Counter failedCounter;

    public OrderService(OrderRepository orderRepository,
                        InventoryService inventoryService,
                        FakePaymentClient paymentClient,
                        ObservationRegistry observationRegistry,
                        Tracer tracer,
                        MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.paymentClient = paymentClient;
        this.observationRegistry = observationRegistry;
        this.tracer = tracer;
        this.createdCounter = Counter.builder("order.created.count")
                .description("Number of successfully created orders")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("order.failed.count")
                .description("Number of failed order creations")
                .register(meterRegistry);
    }

    @Observed(name = "order.create", contextualName = "OrderService.createOrder")
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, boolean failPayment) {
        String orderId = UUID.randomUUID().toString();
        tagCreateSpan(orderId, request);

        inventoryService.reserve(request.items());

        try {
            paymentClient.authorize(orderId, failPayment);
        } catch (PaymentAuthorizationException ex) {
            failedCounter.increment();
            throw ex;
        }

        Order order = new Order(orderId, request.userId(), OrderStatus.CREATED, Instant.now());
        request.items().forEach(item -> order.addItem(item.sku(), item.quantity()));
        insertOrder(order);

        createdCounter.increment();
        log.info("order created", net.logstash.logback.argument.StructuredArguments.kv("order_id", orderId));
        return new CreateOrderResponse(orderId, toStatusString(order.getStatus()));
    }

    @Observed(name = "order.get", contextualName = "OrderService.getOrder")
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        List<OrderResponse.Item> items = order.getItems().stream()
                .map(item -> new OrderResponse.Item(item.getSku(), item.getQuantity()))
                .toList();

        return new OrderResponse(order.getId(), order.getUserId(), toStatusString(order.getStatus()), items);
    }

    /**
     * OrderRepository is a Spring Data interface, so we wrap the save call in an
     * Observation to produce an explicit "OrderRepository.insert" span.
     */
    private void insertOrder(Order order) {
        Observation.createNotStarted("order.repository.insert", observationRegistry)
                .contextualName("OrderRepository.insert")
                .lowCardinalityKeyValue("db.system", "postgresql")
                .lowCardinalityKeyValue("db.operation", "insert")
                .observe(() -> orderRepository.save(order));
    }

    private void tagCreateSpan(String orderId, CreateOrderRequest request) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag("order.id", orderId);
            span.tag("user.id", request.userId());
            span.tag("item.count", String.valueOf(request.items().size()));
        }
    }

    private static String toStatusString(OrderStatus status) {
        return status.name().toLowerCase();
    }
}
