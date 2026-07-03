package com.sangjinsu.monolithotellab.payment;

public class PaymentAuthorizationException extends RuntimeException {

    public PaymentAuthorizationException(String message) {
        super(message);
    }
}
