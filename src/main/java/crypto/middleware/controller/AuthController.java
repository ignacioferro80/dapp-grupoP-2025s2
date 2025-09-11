package crypto.middleware.controller;

import crypto.middleware.dtos.LoginRequest;
import crypto.middleware.dtos.RegisterRequest;
import crypto.middleware.model.User;
import crypto.middleware.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (isBlank(req.getEmail()) || isBlank(req.getPassword())) {
            return ResponseEntity.badRequest().body(new ErrorMsg("Email y contraseña son requeridos"));
        }
        try {
            User u = userService.register(req.getEmail(), req.getPassword(), req.getRole());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SimpleUser(u.getEmail(), u.getRole()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMsg(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (isBlank(req.getEmail()) || isBlank(req.getPassword())) {
            return ResponseEntity.badRequest().body(new ErrorMsg("Email y contraseña son requeridos"));
        }
        try {
            String token = userService.loginAndIssueToken(req.getEmail(), req.getPassword());
            // Para la respuesta necesitamos el role: lo incluimos dentro del token (claim "role")
            // pero también lo devolvemos en claro:
            String role = "user"; // por defecto
            try { role = crypto.middleware.utils.JwtParserShortcut.roleFromToken(token); } catch (Exception ignored) {}

            return ResponseEntity.ok(new AuthResponse(req.getEmail(), role, token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMsg(e.getMessage()));
        }
    }

    private boolean isBlank(String s){ return s == null || s.trim().isEmpty(); }

    // DTOs simples para respuestas de error/usuario
    static class ErrorMsg { public final String message; ErrorMsg(String m){ this.message = m; } }
    static class SimpleUser { public final String email, role; SimpleUser(String e,String r){ email=e; role=r; } }
}
