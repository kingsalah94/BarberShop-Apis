package com.salahtech.BarberShop_Apis.AOP;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // On la met sur des méthodes
@Retention(RetentionPolicy.RUNTIME) // Elle reste active pendant que l'app tourne
public @interface LogActivity {
    String action(); // Ex: "CREATE", "UPDATE", "DELETE"

    String entityName(); // Ex: "Booking", "Client", "Barber"
}