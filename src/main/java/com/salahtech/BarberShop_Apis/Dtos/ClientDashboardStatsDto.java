package com.salahtech.BarberShop_Apis.Dtos;

import lombok.*;

/**
 * DTO for Client Dashboard Statistics
 * Provides comprehensive overview of client activity and status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDashboardStatsDto {

    // ===== BOOKING STATISTICS =====

    /**
     * Number of upcoming bookings (CONFIRMED + PENDING)
     */
    private Long upcomingBookingsCount;

    /**
     * Number of completed bookings
     */
    private Long completedBookingsCount;

    /**
     * Number of cancelled bookings
     */
    private Long cancelledBookingsCount;

    /**
     * Total number of bookings (all statuses)
     */
    private Long totalBookingsCount;

    // ===== FAVORITES & REVIEWS =====

    /**
     * Number of favorite barbers
     */
    private Integer favoriteBarbersCount;

    /**
     * Number of reviews left by client
     */
    private Integer reviewsLeftCount;

    // ===== LOYALTY PROGRAM =====

    /**
     * Current loyalty points balance
     */
    private Integer loyaltyPoints;

    /**
     * Current loyalty tier (BRONZE, SILVER, GOLD, PLATINUM)
     */
    private String loyaltyTier;

    /**
     * Points needed to reach next tier
     */
    private Integer pointsToNextTier;

    // ===== MOST VISITED BARBER =====

    /**
     * Name of most visited barber
     */
    private String mostVisitedBarberName;

    /**
     * Number of visits to most visited barber
     */
    private Long mostVisitedBarberVisitCount;

    // ===== FINANCIAL STATISTICS =====

    /**
     * Total amount spent on all completed bookings
     */
    private Double totalSpent;

    /**
     * Average price per booking
     */
    private Double averageBookingPrice;

    // ===== ENGAGEMENT METRICS =====

    /**
     * Client engagement level based on activity
     * HIGH, MEDIUM, LOW
     */
    @Builder.Default
    private String engagementLevel = "MEDIUM";

    /**
     * Percentage of bookings completed vs total
     * Useful for tracking reliability
     */
    public Double getCompletionRate() {
        if (totalBookingsCount == null || totalBookingsCount == 0) {
            return 0.0;
        }
        return (completedBookingsCount.doubleValue() / totalBookingsCount.doubleValue()) * 100;
    }

    /**
     * Percentage of completed bookings that were reviewed
     * Useful for tracking engagement
     */
    public Double getReviewRate() {
        if (completedBookingsCount == null || completedBookingsCount == 0) {
            return 0.0;
        }
        return (reviewsLeftCount.doubleValue() / completedBookingsCount.doubleValue()) * 100;
    }

    // ===== HELPER METHODS =====

    /**
     * Check if client has any upcoming bookings
     */
    public boolean hasUpcomingBookings() {
        return upcomingBookingsCount != null && upcomingBookingsCount > 0;
    }

    /**
     * Check if client has completed at least one booking
     */
    public boolean hasCompletedBookings() {
        return completedBookingsCount != null && completedBookingsCount > 0;
    }

    /**
     * Check if client is active (has bookings in last 30 days)
     * Note: Would need lastBookingDate field for full implementation
     */
    public boolean isActive() {
        return hasUpcomingBookings() || (completedBookingsCount != null && completedBookingsCount > 0);
    }

    /**
     * Get loyalty progress percentage to next tier
     */
    public Double getLoyaltyProgressPercentage() {
        if (pointsToNextTier == null || pointsToNextTier == 0) {
            return 100.0; // Already at max tier
        }

        // Calculate based on current tier thresholds
        int currentTierPoints = getCurrentTierMinPoints();
        int nextTierPoints = currentTierPoints + pointsToNextTier;
        int progressPoints = loyaltyPoints - currentTierPoints;

        return ((double) progressPoints / (double) pointsToNextTier) * 100;
    }

    /**
     * Get minimum points for current tier
     */
    private int getCurrentTierMinPoints() {
        if (loyaltyTier == null)
            return 0;

        return switch (loyaltyTier) {
            case "BRONZE" -> 0;
            case "SILVER" -> 100;
            case "GOLD" -> 500;
            case "PLATINUM" -> 1000;
            default -> 0;
        };
    }
}
