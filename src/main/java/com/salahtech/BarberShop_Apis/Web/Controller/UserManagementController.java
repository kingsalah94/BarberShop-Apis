package com.salahtech.BarberShop_Apis.Web.Controller;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.database.annotations.NotNull;
import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Dtos.ClientDto;
import com.salahtech.BarberShop_Apis.Dtos.CreateUserWithProfileRequest;
import com.salahtech.BarberShop_Apis.Dtos.UserProfileDto;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Services.UserManagementService;

import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;

@RestController
@RequestMapping("/v1/users")
public class UserManagementController {

    private UserManagementService userService;

    @PostMapping("/complete-profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> createUserWithProfile(
            @Valid @RequestBody CreateUserWithProfileRequest request) {

        return ResponseEntity.ok(userService.createUserWithProfile(request));
    }
}
