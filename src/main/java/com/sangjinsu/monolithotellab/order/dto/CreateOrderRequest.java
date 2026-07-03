package com.sangjinsu.monolithotellab.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String userId,
        @NotEmpty @Valid List<Item> items
) {
    public record Item(
            @NotBlank String sku,
            @Positive int quantity
    ) {
    }
}
