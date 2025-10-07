package predictions.dapp.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import predictions.dapp.model.User;
import predictions.dapp.repositories.UserRepository;

@Configuration
public class DatabaseSeeder {
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

                User user = new User();
                user.setUsername("testuser");
                user.setEmail("testuser@gmail.com");
                user.setPasswordHash(passwordEncoder.encode("test1234")); // encripta la password

                userRepository.save(user);
                System.out.println("   Usuario de prueba insertado en HSQLDB:");
                System.out.println("   Email: testuser@gmail.com");
                System.out.println("   Password: test1234");
            } else {
                System.out.println("ℹLa base ya contiene usuarios, no se insertó el seeder.");
            }
        };
    }
}
