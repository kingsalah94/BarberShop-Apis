package com.salahtech.BarberShop_Apis.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationUserType {
     CLIENT, BARBER, SALON_OWNER, ADMIN;

     @JsonCreator
     public static ApplicationUserType from(Object raw) {
          if (raw == null)
               return CLIENT; // défaut
          String v = raw.toString().trim().toUpperCase();

          switch (v) {
               case "CLIENT":
               case "CUSTOMER":
               case "USER":
                    return CLIENT;

               case "BARBER":
                    return BARBER;

               case "SALON_OWNER":
               case "OWNER":
               case "SALONOWNER":
                    return SALON_OWNER;

               case "ADMIN":
               case "ADMINISTRATOR":
                    return ADMIN;

               default:
                    throw new IllegalArgumentException("Unknown userType: " + raw);
          }
     }

     @JsonValue
     public String toJson() {
          return name();
     }
}
