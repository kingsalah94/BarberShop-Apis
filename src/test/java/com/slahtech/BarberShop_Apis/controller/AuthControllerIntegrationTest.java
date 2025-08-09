
// import com.barbershop.model.dto.AuthRequestDTO;
// import com.barbershop.model.dto.RegisterRequestDTO;
// import com.barbershop.model.entity.Role;
// import com.barbershop.model.entity.User;
// import com.barbershop.repository.RoleRepository;
// import com.barbershop.repository.UserRepository;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.http.MediaType;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.Set;

// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest
// @AutoConfigureWebMvc
// @ActiveProfiles("test")
// @Transactional
// class AuthControllerIntegrationTest {
    
//     @Autowired
//     private MockMvc mockMvc;
    
//     @Autowired
//     private ObjectMapper objectMapper;
    
//     @Autowired
//     private UserRepository userRepository;
    
//     @Autowired
//     private RoleRepository roleRepository;
    
//     @Autowired
//     private PasswordEncoder passwordEncoder;
    
//     private User testUser;
//     private Role clientRole;
    
//     @BeforeEach
//     void setUp() {
//         // Créer un rôle de test
//         clientRole = new Role("CLIENT", "Client role");
//         clientRole = roleRepository.save(clientRole);
        
//         // Créer un utilisateur de test
//         testUser = new User("test@example.com", passwordEncoder.encode("password123"), "John", "Doe");
//         testUser.setIsEnabled(true);
//         testUser.setIsVerified(true);
//         testUser.setRoles(Set.of(clientRole));
//         testUser = userRepository.save(testUser);
//     }
    
//     @Test
//     void testLogin_Success() throws Exception {
//         AuthRequestDTO loginRequest = new AuthRequestDTO("test@example.com", "password123");
        
//         mockMvc.perform(post("/auth/login")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(objectMapper.writeValueAsString(loginRequest)))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.accessToken").exists())
//                 .andExpect(jsonPath("$.refreshToken").exists())
//                 .andExpect(jsonPath("$.user.email").value("test@example.com"));
//     }
    
//     @Test
//     void testLogin_InvalidCredentials() throws Exception {
//         AuthRequestDTO loginRequest = new AuthRequestDTO("test@example.com", "wrongPassword");
        
//         mockMvc.perform(post("/auth/login")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(objectMapper.writeValueAsString(loginRequest)))
//                 .andExpect(status().isUnauthorized());
//     }
    
//     @Test
//     void testRegister_Success() throws Exception {
//         RegisterRequestDTO registerRequest = new RegisterRequestDTO();
//         registerRequest.setEmail("newuser@example.com");
//         registerRequest.setPassword("password123");
//         registerRequest.setFirstName("Jane");
//         registerRequest.setLastName("Smith");
//         registerRequest.setUserType("CLIENT");
        
//         mockMvc.perform(post("/auth/register")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(objectMapper.writeValueAsString(registerRequest)))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.accessToken").exists())
//                 .andExpect(jsonPath("$.user.email").value("newuser@example.com"));
//     }
    
//     @Test
//     void testRegister_EmailAlreadyExists() throws Exception {
//         RegisterRequestDTO registerRequest = new RegisterRequestDTO();
//         registerRequest.setEmail("test@example.com"); // Email déjà existant
//         registerRequest.setPassword("password123");
//         registerRequest.setFirstName("Jane");
//         registerRequest.setLastName("Smith");
//         registerRequest.setUserType("CLIENT");
        
//         mockMvc.perform(post("/auth/register")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(objectMapper.writeValueAsString(registerRequest)))
//                 .andExpect(status().isBadRequest());
//     }
// }
