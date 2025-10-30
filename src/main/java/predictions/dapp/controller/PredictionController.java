package predictions.dapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import predictions.dapp.security.JwtUtil;
import predictions.dapp.service.PredictionService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PredictionController {

    private final PredictionService predictionService;
    private final JwtUtil jwtUtil;

    public PredictionController(PredictionService predictionService, JwtUtil jwtUtil) {
        this.predictionService = predictionService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/prediction")
    public ResponseEntity<Map<String, String>> prediction() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated (not anonymous)
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            Long userId = jwtUtil.extractUserId(email);
            String response = predictionService.handlePrediction(userId);
            return ResponseEntity.ok(Map.of("message", response));
        } else {
            return ResponseEntity.ok(Map.of("message", "Predictions not logged in"));
        }
    }
}