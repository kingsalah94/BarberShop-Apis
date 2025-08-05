package com.salahtech.BarberShop_Apis.Exceptions;

import java.util.List;

import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;

import lombok.Getter;

@Getter
public class InvalideEntityException extends RuntimeException{

    private ErrorCodes errorCodes;
    private List<String> errors;

    public InvalideEntityException(String message){
        super(message);
    }

    public InvalideEntityException(String message,Throwable cause){
        super(message,cause);
    }
    public InvalideEntityException(String message,Throwable cause,ErrorCodes errorCodes){
        super(message,cause);
        this.errorCodes = errorCodes;
    }

    public InvalideEntityException(String message,ErrorCodes errorCodes){
        super(message);
        this.errorCodes = errorCodes;
    }

    public InvalideEntityException(String message,ErrorCodes errorCodes,List<String> errors){
        super(message);
        this.errorCodes = errorCodes;
        this.errors = errors;
    }
}

