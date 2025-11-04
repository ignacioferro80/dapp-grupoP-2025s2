package predictions.dapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import predictions.dapp.security.JwtUtil;
import predictions.dapp.service.PredictionService;
import predictions.dapp.service.TeamPredictionService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PredictionController {

    private final PredictionService predictionService;
    private final TeamPredictionService teamPredictionService;
    private final JwtUtil jwtUtil;

    public PredictionController(PredictionService predictionService,
                                TeamPredictionService teamPredictionService,
                                JwtUtil jwtUtil) {
        this.predictionService = predictionService;
        this.teamPredictionService = teamPredictionService;
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

    /**
     * GET /api/predictions/{teamId1}/{teamId2}
     * Predice el ganador entre dos equipos
     *
     * Comportamiento:
     * - Sin autenticación: Devuelve "User not logged in"
     * - Con autenticación válida: Hace la predicción y guarda en BD
     */
    @GetMapping("/predictions/{teamId1}/{teamId2}")
    public ResponseEntity<?> predictMatchWinner(
            @PathVariable String teamId1,
            @PathVariable String teamId2) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Verificar si el usuario NO está autenticado
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of("message", "User not logged in"));
        }

        // Usuario autenticado - realizar predicción
        try {
            String email = auth.getName();
            Long userId = jwtUtil.extractUserId(email);

            // Realizar predicción y guardar en BD
            Map<String, Object> prediction = teamPredictionService.predictWinner(teamId1, teamId2, userId);
            return ResponseEntity.ok(prediction);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al realizar predicción",
                    "details", e.getMessage()
            ));
        }
    }
}