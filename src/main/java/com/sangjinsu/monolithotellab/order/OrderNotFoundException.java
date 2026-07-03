package com.sangjinsu.monolithotellab.order;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("order not found: " + orderId);
    }
}
