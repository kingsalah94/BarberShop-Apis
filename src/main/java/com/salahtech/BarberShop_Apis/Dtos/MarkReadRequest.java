package com.salahtech.BarberShop_Apis.Dtos;

import lombok.Data;

import java.util.List;

@Data
public class MarkReadRequest {
    private List<Long> messageIds;
}
