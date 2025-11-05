package predictions.dapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import predictions.dapp.model.User;
import predictions.dapp.repositories.UserRepository;

import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class DatabaseSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

                // Generate a secure random password instead of hardcoded one
                String securePassword = generateSecurePassword();

                User user = new User();
                user.setUsername("testuser");
                user.setEmail("testuser@gmail.com");
                user.setPasswordHash(passwordEncoder.encode(securePassword));

                userRepository.save(user);
                logger.info("Test user inserted into HSQLDB:");
                logger.info("Email: testuser@gmail.com");
                logger.info("Password: {}", securePassword);
                logger.warn("IMPORTANT: Change this password in production!");
            } else {
                logger.info("Database already contains users, seeder not executed.");
            }
        };
    }

    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}