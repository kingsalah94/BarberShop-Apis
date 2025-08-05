package com.salahtech.BarberShop_Apis.Exceptions;


public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
