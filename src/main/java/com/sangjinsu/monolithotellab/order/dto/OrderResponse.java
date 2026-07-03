package com.sangjinsu.monolithotellab.order.dto;

import java.util.List;

public record OrderResponse(
        String orderId,
        String userId,
        String status,
        List<Item> items
) {
    public record Item(
            String sku,
            int quantity
    ) {
    }
}
