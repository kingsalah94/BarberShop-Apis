package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.models.Client;

/** Accès de test à la méthode package-private {@link CatalogServiceImpl#publicName}. */
public final class CatalogServiceImplAccess {
    private CatalogServiceImplAccess() {}

    public static String publicName(Client c) {
        return CatalogServiceImpl.publicName(c);
    }
}
