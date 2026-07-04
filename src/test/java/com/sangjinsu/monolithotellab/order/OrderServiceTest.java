package com.sangjinsu.monolithotellab.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sangjinsu.monolithotellab.inventory.InventoryService;
import com.sangjinsu.monolithotellab.order.dto.CreateOrderRequest;
import com.sangjinsu.monolithotellab.order.dto.CreateOrderResponse;
import com.sangjinsu.monolithotellab.order.dto.OrderResponse;
import com.sangjinsu.monolithotellab.payment.FakePaymentClient;
import com.sangjinsu.monolithotellab.payment.PaymentAuthorizationException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Layer 1 of this repo's test pyramid: a pure unit test with Mockito.
 * Observability lesson: instrumentation is neutralized with ObservationRegistry.NOOP
 * and SimpleMeterRegistry, and the Tracer is mocked — business logic stays testable
 * without any tracing backend running.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    InventoryService inventoryService;

    @Mock
    FakePaymentClient paymentClient;

    @Mock
    Tracer tracer;

    OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, inventoryService, paymentClient,
                ObservationRegistry.NOOP, tracer, new SimpleMeterRegistry());
    }

    private CreateOrderRequest sampleRequest() {
        return new CreateOrderRequest("user-1", List.of(new CreateOrderRequest.Item("item-1", 2)));
    }

    @Test
    void createOrder_success_persistsAndReturnsCreated() {
        CreateOrderResponse response = orderService.createOrder(sampleRequest(), false);

        assertThat(response.orderId()).isNotBlank();
        assertThat(response.status()).isEqualTo("created");

        verify(inventoryService).reserve(anyList());
        verify(paymentClient).authorize(anyString(), eq(false));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getSku()).isEqualTo("item-1");
    }

    @Test
    void createOrder_paymentFailure_doesNotPersist() {
        doThrow(new PaymentAuthorizationException("payment authorization failed"))
                .when(paymentClient).authorize(anyString(), eq(true));

        assertThatThrownBy(() -> orderService.createOrder(sampleRequest(), true))
                .isInstanceOf(PaymentAuthorizationException.class)
                .hasMessage("payment authorization failed");

        verify(inventoryService).reserve(anyList());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrder_found_returnsResponse() {
        Order order = new Order("order-1", "user-1", OrderStatus.CREATED, Instant.now());
        order.addItem("item-1", 2);
        when(orderRepository.findWithItemsById("order-1")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder("order-1");

        assertThat(response.orderId()).isEqualTo("order-1");
        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.status()).isEqualTo("created");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).sku()).isEqualTo("item-1");
    }

    @Test
    void getOrder_notFound_throws() {
        when(orderRepository.findWithItemsById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder("missing"))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
