package predictions.dapp.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "History", description = "User query history management - retrieves predictions and performance data history")
public class HistoryController {

    private final HistoryService historyService;
    private final JwtUtil jwtUtil;

    public HistoryController(HistoryService historyService, JwtUtil jwtUtil) {
        this.historyService = historyService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/history")
    @Operation(
            summary = "Get user query history",
            description = "Retrieves the complete history of user queries including all predictions made and performance data requested. " +
                    "Returns separate arrays for predictions and performance queries. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user history",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ObjectNode.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"Predictions\": [\n" +
                                            "    {\n" +
                                            "      \"probabilidad_Arsenal\": \"65.42%\",\n" +
                                            "      \"probabilidad_Chelsea\": \"34.58%\",\n" +
                                            "      \"prediction\": \"Arsenal con 65.42%\",\n" +
                                            "      \"timestamp\": \"Wed Nov 06 10:30:00 ART 2024\"\n" +
                                            "    }\n" +
                                            "  ],\n" +
                                            "  \"Performance\": [\n" +
                                            "    {\n" +
                                            "      \"id\": 44,\n" +
                                            "      \"name\": \"Harry Kane\",\n" +
                                            "      \"team\": \"FC Bayern München\",\n" +
                                            "      \"goals\": 12,\n" +
                                            "      \"matches\": 10,\n" +
                                            "      \"performance\": 1.2,\n" +
                                            "      \"competition\": \"Bundesliga\"\n" +
                                            "    }\n" +
                                            "  ]\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"User not logged in\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "No history data available",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"Predictions\": \"No data\",\n" +
                                            "  \"Performance\": \"No data\"\n" +
                                            "}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> history() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            Long userId = jwtUtil.extractUserId(email);
            ObjectNode response = historyService.getHistory(userId);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.ok(Map.of("message", "User not logged in"));
        }
    }
}