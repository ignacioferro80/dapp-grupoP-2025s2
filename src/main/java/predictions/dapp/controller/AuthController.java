package predictions.dapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.authentication.BadCredentialsException;
import predictions.dapp.dtos.LoginRequest;
import predictions.dapp.dtos.RegisterRequest;
import predictions.dapp.service.MetricsService;
import predictions.dapp.service.UserService;
import predictions.dapp.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication management APIs for user registration and login")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final MetricsService metricsService;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil, MetricsService metricsService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.metricsService = metricsService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and generates a unique API key for the user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"apiKey\": \"123e4567-e89b-12d3-a456-426614174000\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Email already in use or invalid request data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"error\": \"Email already in use\"}"
                            )
                    )
            )
    })
    public ResponseEntity<Object> register(@RequestBody RegisterRequest request) {
        metricsService.incrementRequests();
        return metricsService.measureLatency(() -> {
            try {
                String apiKey = userService.registerUser(request);
                return ResponseEntity.ok(Map.of("apiKey", apiKey));
            } catch (Exception e) {
                metricsService.incrementErrors();
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        });
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user with email and password, returns a JWT token for subsequent requests"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"error\": \"Invalid email or password\"}"
                            )
                    )
            )
    })
    public ResponseEntity<Object> login(@RequestBody LoginRequest request) {
        metricsService.incrementRequests();
        return metricsService.measureLatency(() -> {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
                );
                UserDetails user = userService.loadUserByUsername(request.getEmail());
                String token = jwtUtil.generateToken(user);
                return ResponseEntity.ok(Map.of("token", token));
            } catch (BadCredentialsException e) {
                metricsService.incrementErrors();
                return ResponseEntity.status(403).body(Map.of("error", "Invalid email or password"));
            }
        });
    }

}