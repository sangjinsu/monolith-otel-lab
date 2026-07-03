package com.sangjinsu.monolithotellab.order.dto;

public record CreateOrderResponse(
        String orderId,
        String status
) {
}
