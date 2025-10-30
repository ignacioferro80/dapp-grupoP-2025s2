package predictions.dapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import predictions.dapp.security.JwtUtil;
import predictions.dapp.service.PerformanceService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final JwtUtil jwtUtil;

    public PerformanceController(PerformanceService performanceService, JwtUtil jwtUtil) {
        this.performanceService = performanceService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<String, String>> performance() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated (not anonymous)
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            Long userId = jwtUtil.extractUserId(email);
            String response = performanceService.handlePerformance(userId);
            return ResponseEntity.ok(Map.of("message", response));
        } else {
            return ResponseEntity.ok(Map.of("message", "Performance not logged in"));
        }
    }
}