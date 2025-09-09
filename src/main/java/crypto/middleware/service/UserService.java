package crypto.middleware.service;

import crypto.middleware.dtos.RegisterRequest;
import crypto.middleware.model.ApiKey;
import crypto.middleware.model.User;
import crypto.middleware.repositories.ApiKeyRepository;
import crypto.middleware.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       ApiKeyRepository apiKeyRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user and persist ApiKey
     */
    public String registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        logger.info("Registering user with email: " + request.getEmail());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        logger.info("User password hashed: " + user.getPasswordHash());

        // Save user first
        userRepository.save(user);

        logger.info("User saved with ID: " + user.getId());

        // Generate API Key
        String key = UUID.randomUUID().toString();
        ApiKey apiKey = new ApiKey();
        apiKey.setKey(key);
        apiKey.setUser(user);

        apiKeyRepository.save(apiKey);

        logger.info("API Key generated and saved: " + key);

        return key;
    }

    /**
     * Needed by Spring Security for authentication
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("USER") // can later map roles
                .build();
    }
}

