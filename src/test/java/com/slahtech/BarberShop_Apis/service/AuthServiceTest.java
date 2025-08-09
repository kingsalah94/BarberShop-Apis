


// import com.salahtech.BarberShop_Apis.Dtos.AuthRequestDTO;
// import com.salahtech.BarberShop_Apis.Dtos.AuthResponseDTO;
// import com.salahtech.BarberShop_Apis.Services.ApplicationUserService;
// import com.salahtech.BarberShop_Apis.Services.EmailService;
// import com.salahtech.BarberShop_Apis.Services.Implementations.AuthService;
// import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
// import com.salahtech.BarberShop_Apis.models.ApplicationUser;
// import com.salahtech.BarberShop_Apis.models.Role;
// import com.salahtech.BarberShop_Apis.reppsitories.RoleRepository;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.crypto.password.PasswordEncoder;

// import java.util.Optional;
// import java.util.Set;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class AuthServiceTest {
    
//     @Mock
//     private AuthenticationManager authenticationManager;
    
//     @Mock
//     private ApplicationUserService userRepository;
    
//     @Mock
//     private RoleRepository roleRepository;
    
//     @Mock
//     private PasswordEncoder passwordEncoder;
    
//     @Mock
//     private JwtUtil jwtUtil;
    
//     @Mock
//     private EmailService emailService;
    
//     @InjectMocks
//     private AuthService authService;
    
//     private ApplicationUser testUser;
//     private Role testRole;
    
//     @BeforeEach
//     void setUp() {
//         testRole = new Role("CLIENT", "Client role");
//         testRole.setId(1L);
        
//         testUser = new ApplicationUser("test@example.com", "password", "John", "Doe");
//         testUser.setId(1L);
//         testUser.setIsEnabled(true);
//         testUser.setIsVerified(true);
//         testUser.setRoles(Set.of(testRole));
//     }
    
//     @Test
//     void testLogin_Success() {
//         // Given
//         AuthRequestDTO authRequest = new AuthRequestDTO("test@example.com", "password");
//         Authentication authentication = mock(Authentication.class);
        
//         when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                 .thenReturn(authentication);
//         when(authentication.getPrincipal()).thenReturn(testUser);
//         when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
//         when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
//         when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");
//         when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);
        
//         // When
//         AuthResponseDTO response = authService.login(authRequest);
        
//         // Then
//         assertNotNull(response);
//         assertEquals("access-token", response.getAccessToken());
//         assertEquals("refresh-token", response.getRefreshToken());
//         assertNotNull(response.getUser());
//         assertEquals("test@example.com", response.getUser().getEmail());
        
//         verify(userRepository).save(testUser);
//     }
    
//     @Test
//     void testLogin_UserNotVerified() {
//         // Given
//         testUser.setIsVerified(false);
//         AuthRequestDTO authRequest = new AuthRequestDTO("test@example.com", "password");
//         Authentication authentication = mock(Authentication.class);
        
//         when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                 .thenReturn(authentication);
//         when(authentication.getPrincipal()).thenReturn(testUser);
//         when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        
//         // When & Then
//         assertThrows(BadRequestException.class, () -> {
//             authService.login(authRequest);
//         });
//     }
    
//     @Test
//     void testRegister_Success() {
//         // Given
//         RegisterRequestDTO registerRequest = new RegisterRequestDTO();
//         registerRequest.setEmail("newuser@example.com");
//         registerRequest.setPassword("password123");
//         registerRequest.setFirstName("Jane");
//         registerRequest.setLastName("Smith");
//         registerRequest.setUserType("CLIENT");
        
//         when(userRepository.existsByEmail(anyString())).thenReturn(false);
//         when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
//         when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(testRole));
//         when(userRepository.save(any(User.class))).thenReturn(testUser);
//         when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
//         when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");
//         when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);
        
//         // When
//         AuthResponseDTO response = authService.register(registerRequest);
        
//         // Then
//         assertNotNull(response);
//         assertEquals("access-token", response.getAccessToken());
//         verify(emailService).sendVerificationEmail(any(User.class));
//         verify(userRepository).save(any(User.class));
//     }
    
//     @Test
//     void testRegister_EmailAlreadyExists() {
//         // Given
//         RegisterRequestDTO registerRequest = new RegisterRequestDTO();
//         registerRequest.setEmail("existing@example.com");
        
//         when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
//         // When & Then
//         assertThrows(BadRequestException.class, () -> {
//             authService.register(registerRequest);
//         });
//     }
// }
