package com.salahtech.BarberShop_Apis.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Non-régression : equals/hashCode/toString Lombok sur des relations
 * bidirectionnelles provoquaient un StackOverflowError (→ 500 masqué).
 */
class EntityEqualityTest {

    @Test
    void relationBidirectionnelle_sansRecursionInfinie() {
        Salon salon = new Salon();
        salon.setId(1L);
        Workstation ws = Workstation.builder().id(2L).salon(salon).build();
        salon.setWorkstations(List.of(ws));

        assertThatCode(() -> {
            salon.hashCode();
            ws.hashCode();
            salon.toString();
            ws.toString();
            salon.equals(new Salon());
        }).doesNotThrowAnyException();
    }

    @Test
    void egaliteParIdentifiant() {
        Salon a = new Salon();
        a.setId(5L);
        a.setName("A");
        Salon b = new Salon();
        b.setId(5L);
        b.setName("B (modifié)");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
