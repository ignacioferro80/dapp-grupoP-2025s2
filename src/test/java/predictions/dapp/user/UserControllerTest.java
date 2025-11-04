package predictions.dapp.user;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import predictions.dapp.model.User;
import predictions.dapp.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Tag("unit")
    @Test
    void testRegisterUser_Success() throws Exception {
        Map<String, String> request = Map.of(
                "email", "test@example.com",
                "password", "123456",
                "name", "Nacho"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Tag("unit")
    @Test
    void testRegisterUser_EmailAlreadyExists() throws Exception {
        // Usuario previo
        User user = new User();
        user.setEmail("duplicate@example.com");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setUsername("Nacho");
        userRepository.save(user);

        Map<String, String> request = Map.of(
                "email", "duplicate@example.com",
                "password", "123456",
                "name", "Nacho"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Tag("unit")
    @Test
    void testLoginUser_Success() throws Exception {
        User user = new User();
        user.setEmail("login@example.com");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setUsername("Nacho");
        userRepository.save(user);

        Map<String, String> request = Map.of(
                "email", "login@example.com",
                "password", "123456"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Tag("unit")
    @Test
    void testLoginUser_InvalidCredentials() throws Exception {
        Map<String, String> request = Map.of(
                "email", "wrong@example.com",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}