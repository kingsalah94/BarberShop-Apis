package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.LoyaltyStatusDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Règle « 10 visites payées = 1 prestation offerte ». */
class LoyaltyRuleTest {

    @Test
    void nineVisits_noRewardYet() {
        LoyaltyStatusDto s = LoyaltyService.build(1L, "S", 9, 0);
        assertThat(s.getStamps()).isEqualTo(9);
        assertThat(s.getRewardsAvailable()).isZero();
    }

    @Test
    void tenVisits_oneRewardAvailable_cardRestarts() {
        LoyaltyStatusDto s = LoyaltyService.build(1L, "S", 10, 0);
        assertThat(s.getRewardsAvailable()).isEqualTo(1);
        assertThat(s.getStamps()).isZero();
    }

    @Test
    void freeVisitDoesNotCountAsPaid() {
        // 10 payées + 1 offerte terminée (11 visites terminées, 1 récompense utilisée)
        LoyaltyStatusDto s = LoyaltyService.build(1L, "S", 11, 1);
        assertThat(s.getPaidVisits()).isEqualTo(10);
        assertThat(s.getRewardsAvailable()).isZero();
        assertThat(s.getStamps()).isZero();
    }

    @Test
    void twentyThreePaid_twoEarned_oneUsed() {
        LoyaltyStatusDto s = LoyaltyService.build(1L, "S", 24, 1);
        assertThat(s.getPaidVisits()).isEqualTo(23);
        assertThat(s.getRewardsEarned()).isEqualTo(2);
        assertThat(s.getRewardsAvailable()).isEqualTo(1);
        assertThat(s.getStamps()).isEqualTo(3);
    }
}
