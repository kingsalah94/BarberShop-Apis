package com.salahtech.BarberShop_Apis.models;


import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.salahtech.BarberShop_Apis.Enums.AuthProvider;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@Entity
//@Builder
@Table(name = "users")
public class ApplicationUser implements UserDetails {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    @Email(message = "Format d'email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;
    
    @Column(nullable = false)
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;
    
    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;
    
    @Column(unique = true)
    private String phone;
    
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;
    
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
    
    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "avatar_url")
    private String profileImageUrl;

    @Column(columnDefinition = "TEXT")
    private String preferences;
    
    @Column(name = "reset_password_token")
    private String resetPasswordToken;
    
    @Column(name = "reset_password_expires")
    private LocalDateTime resetPasswordExpires;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private AuthProvider provider = AuthProvider.LOCAL;
    
    @Column(name = "provider_id")
    private String providerId;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    // Relation OneToOne avec Barber
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Barber barber;

    // One user can have many bookings
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings;
    
   
    
    
    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toList());
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
    

    // Default constructor for JPA
    public ApplicationUser() {}
    
    public ApplicationUser(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    // public Long getId() { return id; }
    // public void setId(Long id) { this.id = id; }
    
    // public String getEmail() { return email; }
    // public void setEmail(String email) { this.email = email; }
    
    // public String getPassword() { return password; }
    // public void setPassword(String password) { this.password = password; }
    
    // public String getFirstName() { return firstName; }
    // public void setFirstName(String firstName) { this.firstName = firstName; }
    
    // public String getLastName() { return lastName; }
    // public void setLastName(String lastName) { this.lastName = lastName; }
    
    // public String getPhone() { return phone; }
    // public void setPhone(String phone) { this.phone = phone; }
    
    // public Boolean getIsEnabled() { return isEnabled; }
    // public void setIsEnabled(Boolean enabled) { isEnabled = enabled; }
    
    // public Boolean getIsVerified() { return isVerified; }
    // public void setIsVerified(Boolean verified) { isVerified = verified; }
    
    // public String getVerificationToken() { return verificationToken; }
    // public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    
    // public String getResetPasswordToken() { return resetPasswordToken; }
    // public void setResetPasswordToken(String resetPasswordToken) { this.resetPasswordToken = resetPasswordToken; }
    
    // public LocalDateTime getResetPasswordExpires() { return resetPasswordExpires; }
    // public void setResetPasswordExpires(LocalDateTime resetPasswordExpires) { this.resetPasswordExpires = resetPasswordExpires; }
    
    // public LocalDateTime getCreatedAt() { return createdAt; }
    // public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // public LocalDateTime getUpdatedAt() { return updatedAt; }
    // public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // public LocalDateTime getLastLogin() { return lastLogin; }
    // public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    // public AuthProvider getProvider() { return provider; }
    // public void setProvider(AuthProvider provider) { this.provider = provider; }
    
    // public String getProviderId() { return providerId; }
    // public void setProviderId(String providerId) { this.providerId = providerId; }
    
    // public Set<Role> getRoles() { return roles; }
    // public void setRoles(Set<Role> roles) { this.roles = roles; }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAccountNonExpired'");
    }

	public void setAccountNonLocked(boolean isAccountNonLocked) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'setAccountNonLocked'");
	}

	public void setCredentialsNonExpired(boolean credentialsNonExpired) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'setCredentialsNonExpired'");
	}

    //old code
    //  @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    // private Long id;
    

    // @Size(max = 100)
    // @Column(name = "first_name", nullable = false)
    // @NotBlank(message = "Le prénom est obligatoire")
    // private String firstName;

    
    // @Size(max = 100)
    // @Column(name = "last_name", nullable = false)
    // @NotBlank(message = "Le nom est obligatoire")
    // private String lastName;
    
    // @Column(unique = true, nullable = false)
    // @Email(message = "Format d'email invalide")
    // @NotBlank(message = "L'email est obligatoire")
    // private String email;
    
  
    // @Column(name = "phone_number", nullable = false, unique = true)
    // @NotBlank(message = "Le numéro de téléphone est obligatoire")
    // @Size(min = 10, max = 20, message = "Le numéro de téléphone doit contenir entre 10 et 20 caractères")
    // private String phoneNumber;
    
    // @Column(nullable = false)
    // @NotBlank(message = "Le mot de passe est obligatoire")
    // @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    // private String password;
    
    
    // @Column(name = "avatar_url")
    // private String profileImageUrl;
    
    // @Column(columnDefinition = "TEXT")
    // private String preferences;
    
    // @Column(name = "created_at", nullable = false, updatable = false)
    // private LocalDateTime createdAt;
    
    // @Column(name = "updated_at")
    // private LocalDateTime updatedAt;
    
    // @Column(name = "account_non_expired")
    // private boolean accountNonExpired = true;
    
    // @Column(name = "account_non_locked")
    // private boolean accountNonLocked = true;
    
    // @Column(name = "credentials_non_expired")
    // private boolean credentialsNonExpired = true;
    
    // @Column(name = "enabled")
    // private boolean isEnabled = false;

    // @Column(name = "is_verified", nullable = false)
    // private Boolean isVerified = false;

    // @Column(name = "verification_token")
    // private String verificationToken;

    // @Column(name = "reset_password_token")
    // private String resetPasswordToken;

    // @Column(name = "reset_password_expires")
    // private LocalDateTime resetPasswordExpires;

    //  @Column(name = "last_login")
    // private LocalDateTime lastLogin;

    // @Enumerated(EnumType.STRING)
    // @Column(name = "provider")
    // private AuthProvider provider = AuthProvider.LOCAL;

    //  @Column(name = "provider_id")
    // private String providerId;

    // @ManyToMany(fetch = FetchType.EAGER)
    // @JoinTable(
    //     name = "user_roles",
    //     joinColumns = @JoinColumn(name = "user_id"),
    //     inverseJoinColumns = @JoinColumn(name = "role_id")
    // )
    // private Set<AuthProvider> roles;

    // // Relation OneToOne avec Barber
    // @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    // private Barber barber;

    // // One user can have many bookings
    // @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Booking> bookings;
    


    
    // @PrePersist
    // protected void onCreate() {
    //     createdAt = LocalDateTime.now();
    //     updatedAt = LocalDateTime.now();
    // }
    
    // @PreUpdate
    // protected void onUpdate() {
    //     updatedAt = LocalDateTime.now();
    // }
    
    // // UserDetails implementation
    
    
    // @Override
    // public String getUsername() {
    //     return email;
    // }
    
    // @Override
    // public boolean isAccountNonExpired() {
    //     return accountNonExpired;
    // }
    
    // @Override
    // public boolean isAccountNonLocked() {
    //     return accountNonLocked;
    // }
    
    // @Override
    // public boolean isCredentialsNonExpired() {
    //     return credentialsNonExpired;
    // }
    
    // @Override
    // public boolean isEnabled() {
    //     return isEnabled;
    // }

    // @Override
    // public String getPassword() {
    //     return this.password;
    // }

    // // @Override
    // // public Collection<? extends GrantedAuthority> getAuthorities() {
    // //     return List.of(new SimpleGrantedAuthority("ROLE_" + Role.getName()));

    // // }

    // public void setAccountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; }
    // public void setAccountNonLocked(boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }
    // public void setCredentialsNonExpired(boolean credentialsNonExpired) { this.credentialsNonExpired = credentialsNonExpired; }
    // public void setEnabled(boolean enabled) { this.isEnabled = enabled; }

    // @Override
    // public Collection<? extends GrantedAuthority> getAuthorities() {
    //     // TODO Auto-generated method stub
    //     throw new UnsupportedOperationException("Unimplemented method 'getAuthorities'");
    // }

}
    
