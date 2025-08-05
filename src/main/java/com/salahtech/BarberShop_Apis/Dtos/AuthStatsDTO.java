package com.salahtech.BarberShop_Apis.Dtos;

public class AuthStatsDTO {
    private long totalUsers;
    private long activeUsers;
    private long clientsCount;
    private long barbersCount;

    public AuthStatsDTO(long totalUsers, long activeUsers, long clientsCount, long barbersCount) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.clientsCount = clientsCount;
        this.barbersCount = barbersCount;
    }

    // Getters et Setters
    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getClientsCount() {
        return clientsCount;
    }

    public void setClientsCount(long clientsCount) {
        this.clientsCount = clientsCount;
    }

    public long getBarbersCount() {
        return barbersCount;
    }

    public void setBarbersCount(long barbersCount) {
        this.barbersCount = barbersCount;
    }
}
