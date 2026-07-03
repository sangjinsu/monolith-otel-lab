package com.sangjinsu.monolithotellab.web;

import com.sangjinsu.monolithotellab.order.OrderService;
import com.sangjinsu.monolithotellab.order.dto.CreateOrderRequest;
import com.sangjinsu.monolithotellab.order.dto.CreateOrderResponse;
import com.sangjinsu.monolithotellab.order.dto.OrderResponse;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Observed(name = "order.controller.create", contextualName = "OrderController.createOrder")
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestParam(name = "fail_payment", defaultValue = "false") boolean failPayment) {
        CreateOrderResponse response = orderService.createOrder(request, failPayment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Observed(name = "order.controller.get", contextualName = "OrderController.getOrder")
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }
}
