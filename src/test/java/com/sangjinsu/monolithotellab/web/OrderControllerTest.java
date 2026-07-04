package com.sangjinsu.monolithotellab.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sangjinsu.monolithotellab.order.OrderNotFoundException;
import com.sangjinsu.monolithotellab.order.OrderService;
import com.sangjinsu.monolithotellab.order.dto.CreateOrderResponse;
import com.sangjinsu.monolithotellab.order.dto.OrderResponse;
import com.sangjinsu.monolithotellab.payment.PaymentAuthorizationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Layer 2: @WebMvcTest slice — only the web layer (controller + advice) is loaded.
 * Verifies the HTTP contract: status codes (201/402/400/404) and the snake_case JSON
 * field names produced by the global Jackson naming strategy in application.yml.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    private static final String BODY =
            "{\"user_id\":\"user-1\",\"items\":[{\"sku\":\"item-1\",\"quantity\":2}]}";

    @Test
    void createOrder_returns201() throws Exception {
        when(orderService.createOrder(any(), eq(false)))
                .thenReturn(new CreateOrderResponse("order-1", "created"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order_id").value("order-1"))
                .andExpect(jsonPath("$.status").value("created"));
    }

    @Test
    void createOrder_paymentFailure_returns402() throws Exception {
        when(orderService.createOrder(any(), eq(true)))
                .thenThrow(new PaymentAuthorizationException("payment authorization failed"));

        mockMvc.perform(post("/orders?fail_payment=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("payment authorization failed"));
    }

    @Test
    void createOrder_invalidRequest_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_returns200() throws Exception {
        when(orderService.getOrder("order-1"))
                .thenReturn(new OrderResponse("order-1", "user-1", "created",
                        List.of(new OrderResponse.Item("item-1", 2))));

        mockMvc.perform(get("/orders/order-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value("order-1"))
                .andExpect(jsonPath("$.user_id").value("user-1"))
                .andExpect(jsonPath("$.items[0].sku").value("item-1"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        when(orderService.getOrder("missing")).thenThrow(new OrderNotFoundException("missing"));

        mockMvc.perform(get("/orders/missing"))
                .andExpect(status().isNotFound());
    }
}
