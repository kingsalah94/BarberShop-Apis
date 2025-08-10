package com.salahtech.BarberShop_Apis.outbox;


public enum OutboxStatus { 
    PENDING, 
    SENDING, 
    SENT, 
    FAILED
}