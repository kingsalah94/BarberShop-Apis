package com.salahtech.BarberShop_Apis.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.PAYMENT_REQUIRED)
public class PaymentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
