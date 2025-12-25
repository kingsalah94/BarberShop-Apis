package com.salahtech.BarberShop_Apis.Config;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.lang.annotation.*;

@Target({ ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "@applicationUserServiceImpl.findByEmail(#this.name)")
// 👆 L'expression SpEL magique qui appelle votre service automatiquement
public @interface CurrentUser {
}
