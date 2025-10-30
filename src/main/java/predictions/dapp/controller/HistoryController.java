package predictions.dapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import predictions.dapp.security.JwtUtil;
import predictions.dapp.service.HistoryService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HistoryController {

    private final HistoryService historyService;
    private final JwtUtil jwtUtil;

    public HistoryController(HistoryService historyService, JwtUtil jwtUtil) {
        this.historyService = historyService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, String>> history() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated (not anonymous)
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            Long userId = jwtUtil.extractUserId(email);
            Map<String, String> response = historyService.getHistory(userId);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.ok(Map.of("message", "User not logged in"));
        }
    }
}