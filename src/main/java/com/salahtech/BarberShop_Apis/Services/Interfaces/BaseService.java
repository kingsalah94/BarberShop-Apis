package com.salahtech.BarberShop_Apis.Services.Interfaces;

import java.util.List;

// ===========================================
// 1. INTERFACE DE BASE GÉNÉRIQUE
// ===========================================
public interface BaseService<T, ID> {
    T save(T dto);
    T findById(ID id);
    List<T> findAll();
    void delete(ID id);
}