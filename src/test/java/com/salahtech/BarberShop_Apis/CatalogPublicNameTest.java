package com.salahtech.BarberShop_Apis;

import com.salahtech.BarberShop_Apis.Services.Implementations.CatalogServiceImplAccess;
import com.salahtech.BarberShop_Apis.models.Client;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Les avis publics n'exposent que le prénom et l'initiale du nom. */
class CatalogPublicNameTest {

    private static Client client(String first, String last) {
        Client c = new Client();
        c.setFirstName(first);
        c.setLastName(last);
        return c;
    }

    @Test
    void prenomEtInitiale() {
        assertThat(CatalogServiceImplAccess.publicName(client("Salah", "Kané"))).isEqualTo("Salah K.");
    }

    @Test
    void casLimites() {
        assertThat(CatalogServiceImplAccess.publicName(null)).isEqualTo("Client");
        assertThat(CatalogServiceImplAccess.publicName(client(" ", null))).isEqualTo("Client");
        assertThat(CatalogServiceImplAccess.publicName(client("Amina", ""))).isEqualTo("Amina");
        assertThat(CatalogServiceImplAccess.publicName(client(null, "diallo"))).isEqualTo("D.");
    }
}
