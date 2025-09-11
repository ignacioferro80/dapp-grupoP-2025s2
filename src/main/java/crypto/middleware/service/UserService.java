package crypto.middleware.service;

import crypto.middleware.utils.JwtUtil;
import crypto.middleware.model.User;
import crypto.middleware.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepo, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    // ► Registro
    public User register(String email, String rawPassword, String role) {
        Optional<User> existing = userRepo.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email ya registrado");
        }
        if (role == null || role.isBlank()) role = "user";

        User u = new User();
        u.setEmail(email.trim().toLowerCase());
        u.setPassword(encoder.encode(rawPassword));
        u.setRole(role.toLowerCase());

        return userRepo.save(u);
    }

    // ► Login → valida contraseña y devuelve token + role
    public String loginAndIssueToken(String email, String rawPassword) {
        User u = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!encoder.matches(rawPassword, u.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        return jwtUtil.generateToken(u.getEmail(), u.getRole());
    }
}
