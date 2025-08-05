package com.salahtech.BarberShop_Apis.Web.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Services.ApplicationUserService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.UserService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ApplicationUserController {

    @Autowired
    private ApplicationUserService userService;
    
    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            ApplicationUser user = userService.findByEmail(email);
            ApplicationUserDto userDTO = convertToUserDTO(user);
            
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ') and (#id == authentication.principal.id or hasAuthority('ADMIN_ACCESS'))")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            ApplicationUser user = userService.findById(id);
            ApplicationUserDto userDTO = convertToUserDTO(user);
            
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    private ApplicationUserDto convertToUserDTO(ApplicationUser user) {
        ApplicationUserDto userDTO = new ApplicationUserDto();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setPhone(user.getPhone());
        userDTO.setIsEnabled(user.getIsEnabled());
        userDTO.setIsVerified(user.getIsVerified());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setLastLogin(user.getLastLogin());
        
        return userDTO;
    }
}
