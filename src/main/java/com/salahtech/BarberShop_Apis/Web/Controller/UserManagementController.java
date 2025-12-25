package com.salahtech.BarberShop_Apis.Web.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.salahtech.BarberShop_Apis.Dtos.CreateUserWithProfileRequest;
import com.salahtech.BarberShop_Apis.Dtos.UserProfileDto;
import com.salahtech.BarberShop_Apis.Services.UserManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userService;

    @PostMapping("/complete-profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> createUserWithProfile(
            @Valid @RequestBody CreateUserWithProfileRequest request) {

        return ResponseEntity.ok(userService.createUserWithProfile(request));
    }
}
