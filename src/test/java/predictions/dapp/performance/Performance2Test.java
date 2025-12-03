package predictions.dapp.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import predictions.dapp.controller.PerformanceController;
import predictions.dapp.security.JwtUtil;
import predictions.dapp.service.MetricsService;
import predictions.dapp.service.PerformanceService;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class Performance2Test {

    @Mock
    private PerformanceService performanceService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private MetricsService metricsService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PerformanceController performanceController;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        // Mock metricsService to execute the callable immediately
        when(metricsService.measureLatency(any())).thenAnswer(invocation -> {
            Object callable = invocation.getArgument(0);
            return ((Callable<?>) callable).call();
        });
    }

    // ==================== AUTHENTICATED USER TESTS ====================

    @Tag("unit")
    @Test
    void testPerformance_AuthenticatedUser_Success() throws Exception {
        // Arrange
        String playerId = "44";
        Long userId = 1L;
        String email = "test@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("id", 44);
        expectedResponse.put("name", "Harry Kane");
        expectedResponse.put("team", "FC Bayern MÃ¼nchen");
        expectedResponse.put("goals", 12);
        expectedResponse.put("matches", 10);
        expectedResponse.put("performance", 1.2);
        expectedResponse.put("competition", "Bundesliga");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ObjectNode);
        ObjectNode body = (ObjectNode) response.getBody();
        assertEquals(44, body.get("id").asInt());
        assertEquals("Harry Kane", body.get("name").asText());
        assertEquals("Bundesliga", body.get("competition").asText());

        verify(metricsService).incrementRequests();
        verify(metricsService).measureLatency(any());
        verify(jwtUtil).extractUserId(email);
        verify(performanceService).handlePerformance(userId, playerId);
        verify(metricsService, never()).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPerformance_AuthenticatedUser_PlayerNotInTopScorers() throws Exception {
        // Arrange
        String playerId = "12345";
        Long userId = 2L;
        String email = "user@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("id", 12345);
        expectedResponse.put("name", "John Smith");
        expectedResponse.put("team", "Example FC");
        expectedResponse.put("performance", "Player 12345 John Smith performance is below average top players");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ObjectNode body = (ObjectNode) response.getBody();
        assertTrue(body.get("performance").asText().contains("below average"));

        verify(performanceService).handlePerformance(userId, playerId);
    }

    @Tag("unit")
    @Test
    void testPerformance_AuthenticatedUser_DifferentPlayerId() throws Exception {
        // Arrange
        String playerId = "777";
        Long userId = 3L;
        String email = "another@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("id", 777);
        expectedResponse.put("name", "Test Player");
        expectedResponse.put("goals", 20);
        expectedResponse.put("matches", 15);
        expectedResponse.put("performance", 1.33);
        expectedResponse.put("competition", "Premier League");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(performanceService).handlePerformance(userId, playerId);
    }

    @Tag("unit")
    @Test
    void testPerformance_AuthenticatedUser_PlayerWithZeroGoals() throws Exception {
        // Arrange
        String playerId = "888";
        Long userId = 4L;
        String email = "test@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("id", 888);
        expectedResponse.put("name", "Defender Player");
        expectedResponse.put("team", "Defense FC");
        expectedResponse.put("goals", 0);
        expectedResponse.put("matches", 20);
        expectedResponse.put("performance", 0.0);
        expectedResponse.put("competition", "Serie A");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ObjectNode body = (ObjectNode) response.getBody();
        assertEquals(0, body.get("goals").asInt());
        assertEquals(0.0, body.get("performance").asDouble(), 0.01);
    }

    // ==================== UNAUTHENTICATED USER TESTS ====================

    @Tag("unit")
    @Test
    void testPerformance_UnauthenticatedUser_NullAuthentication() throws IOException, InterruptedException {
        // Arrange
        String playerId = "44";
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("User not logged in", body.get("message"));

        verify(metricsService).incrementRequests();
        verify(performanceService, never()).handlePerformance(anyLong(), anyString());
        verify(metricsService, never()).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPerformance_UnauthenticatedUser_NotAuthenticated() throws IOException, InterruptedException {
        // Arrange
        String playerId = "44";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("User not logged in", body.get("message"));

        verify(performanceService, never()).handlePerformance(anyLong(), anyString());
    }

    @Tag("unit")
    @Test
    void testPerformance_UnauthenticatedUser_AnonymousUser() throws IOException, InterruptedException {
        // Arrange
        String playerId = "44";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("User not logged in", body.get("message"));

        verify(performanceService, never()).handlePerformance(anyLong(), anyString());
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Tag("unit")
    @Test
    void testPerformance_PerformanceServiceThrowsInterruptedException() throws Exception {
        // Arrange
        String playerId = "44";
        Long userId = 1L;
        String email = "test@example.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId))
                .thenThrow(new InterruptedException("Request interrupted"));

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.get("error").toString().contains("Request interrupted"));

        verify(metricsService).incrementRequests();
        verify(metricsService, never()).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPerformance_PerformanceServiceThrowsIOException() throws Exception {
        // Arrange
        String playerId = "44";
        Long userId = 1L;
        String email = "test@example.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId))
                .thenThrow(new IOException("Failed to fetch competitions"));

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.get("error").toString().contains("Failed to fetch performance data"));
        assertTrue(body.get("error").toString().contains("Failed to fetch competitions"));
    }

    @Tag("unit")
    @Test
    void testPerformance_PerformanceServiceThrowsGenericException() throws Exception {
        // Arrange
        String playerId = "44";
        Long userId = 1L;
        String email = "test@example.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.get("error").toString().contains("Failed to fetch performance data"));
        assertTrue(body.get("error").toString().contains("Database connection failed"));

        verify(metricsService).incrementRequests();
        verify(metricsService, never()).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPerformance_JwtUtilThrowsException() throws IOException, InterruptedException {
        // Arrange
        String playerId = "44";
        String email = "test@example.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenThrow(new RuntimeException("Invalid JWT token"));

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof String);
        assertTrue(response.getBody().toString().contains("Invalid JWT token"));

        verify(metricsService).incrementRequests();
        verify(metricsService).incrementErrors();
        verify(performanceService, never()).handlePerformance(anyLong(), anyString());
    }

    @Tag("unit")
    @Test
    void testPerformance_NullPointerExceptionInAuthentication() {
        // Arrange
        String playerId = "44";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(null);
        // Mock jwtUtil to throw exception when passed null
        when(jwtUtil.extractUserId(null)).thenThrow(new NullPointerException("Email cannot be null"));

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(metricsService).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPerformance_PerformanceDataExceptionFromService() throws Exception {
        // Arrange
        String playerId = "44";
        Long userId = 1L;
        String email = "test@example.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId))
                .thenThrow(new predictions.dapp.exceptions.PerformanceDataException(
                        "Failed to save performance data",
                        new IOException("Database error")
                ));

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.get("error").toString().contains("Failed to fetch performance data"));
    }

    // ==================== EDGE CASES ====================

    @Tag("unit")
    @Test
    void testPerformance_EmptyPlayerId() throws Exception {
        // Arrange
        String playerId = "";
        Long userId = 1L;
        String email = "test@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("error", "Invalid player ID");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(performanceService).handlePerformance(userId, playerId);
    }

    @Tag("unit")
    @Test
    void testPerformance_NonNumericPlayerId() throws Exception {
        // Arrange
        String playerId = "abc123";
        Long userId = 1L;
        String email = "test@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("message", "Player processed");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(performanceService).handlePerformance(userId, playerId);
    }

    @Tag("unit")
    @Test
    void testPerformance_VeryLargePlayerId() throws Exception {
        // Arrange
        String playerId = "999999999";
        Long userId = 1L;
        String email = "test@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("id", 999999999);
        expectedResponse.put("name", "Unknown Player");
        expectedResponse.put("team", "Unknown");
        expectedResponse.put("performance", "Player 999999999 Unknown Player performance is below average top players");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(performanceService).handlePerformance(userId, playerId);
    }

    @Tag("unit")
    @Test
    void testPerformance_MultipleCallsWithSameUser() throws Exception {
        // Arrange
        String playerId1 = "44";
        String playerId2 = "77";
        Long userId = 1L;
        String email = "test@example.com";

        ObjectNode response1 = mapper.createObjectNode();
        response1.put("id", 44);
        response1.put("name", "Player One");
        response1.put("competition", "Bundesliga");

        ObjectNode response2 = mapper.createObjectNode();
        response2.put("id", 77);
        response2.put("name", "Player Two");
        response2.put("competition", "Premier League");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId1)).thenReturn(response1);
        when(performanceService.handlePerformance(userId, playerId2)).thenReturn(response2);

        // Act
        ResponseEntity<Object> result1 = performanceController.performance(playerId1);
        ResponseEntity<Object> result2 = performanceController.performance(playerId2);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(HttpStatus.OK, result1.getStatusCode());
        assertEquals(HttpStatus.OK, result2.getStatusCode());
        verify(metricsService, times(2)).incrementRequests();
        verify(performanceService).handlePerformance(userId, playerId1);
        verify(performanceService).handlePerformance(userId, playerId2);
    }

    @Tag("unit")
    @Test
    void testPerformance_SpecialCharactersInEmail() throws Exception {
        // Arrange
        String playerId = "44";
        Long userId = 1L;
        String email = "test+special@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("id", 44);
        expectedResponse.put("name", "Test Player");
        expectedResponse.put("competition", "Serie A");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jwtUtil).extractUserId(email);
        verify(performanceService).handlePerformance(userId, playerId);
    }

    @Tag("unit")
    @Test
    void testPerformance_MetricsServiceVerification() throws Exception {
        // Arrange
        String playerId = "44";
        Long userId = 1L;
        String email = "test@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("id", 44);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        performanceController.performance(playerId);

        // Assert
        verify(metricsService, times(1)).incrementRequests();
        verify(metricsService, times(1)).measureLatency(any());
        verify(metricsService, never()).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPerformance_NullEmailFromAuthentication() {
        // Arrange
        String playerId = "44";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("test@example.com");
        when(authentication.getName()).thenReturn(null);
        // Mock jwtUtil to throw exception when passed null
        when(jwtUtil.extractUserId(null)).thenThrow(new NullPointerException("Email cannot be null"));

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(metricsService).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPerformance_PlayerWithUnknownTeam() throws Exception {
        // Arrange
        String playerId = "999";
        Long userId = 5L;
        String email = "test@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("id", 999);
        expectedResponse.put("name", "Free Agent Player");
        expectedResponse.put("team", "Unknown");
        expectedResponse.put("performance", "Player 999 Free Agent Player performance is below average top players");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ObjectNode body = (ObjectNode) response.getBody();
        assertEquals("Unknown", body.get("team").asText());
    }

    @Tag("unit")
    @Test
    void testPerformance_HighPerformancePlayer() throws Exception {
        // Arrange
        String playerId = "777";
        Long userId = 6L;
        String email = "test@example.com";

        ObjectNode expectedResponse = mapper.createObjectNode();
        expectedResponse.put("id", 777);
        expectedResponse.put("name", "Super Striker");
        expectedResponse.put("team", "Goals FC");
        expectedResponse.put("goals", 30);
        expectedResponse.put("matches", 15);
        expectedResponse.put("performance", 2.0);
        expectedResponse.put("competition", "Serie A");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
        when(performanceService.handlePerformance(userId, playerId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> response = performanceController.performance(playerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ObjectNode body = (ObjectNode) response.getBody();
        assertEquals(30, body.get("goals").asInt());
        assertEquals(2.0, body.get("performance").asDouble(), 0.01);
    }

    @Tag("unit")
    @Test
    void testPerformance_AllPriorityCompetitions() throws Exception {
        // Arrange
        String[] playerIds = {"1", "2", "3", "4", "5"};
        String[] competitions = {"Serie A", "Premier League", "La Liga", "Ligue 1", "Bundesliga"};
        Long userId = 7L;
        String email = "test@example.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);

        for (int i = 0; i < playerIds.length; i++) {
            ObjectNode response = mapper.createObjectNode();
            response.put("id", Integer.parseInt(playerIds[i]));
            response.put("name", "Player " + i);
            response.put("competition", competitions[i]);
            when(performanceService.handlePerformance(userId, playerIds[i])).thenReturn(response);
        }

        // Act & Assert
        for (int i = 0; i < playerIds.length; i++) {
            ResponseEntity<Object> result = performanceController.performance(playerIds[i]);
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            ObjectNode body = (ObjectNode) result.getBody();
            assertEquals(competitions[i], body.get("competition").asText());
        }

        verify(metricsService, times(5)).incrementRequests();
    }
}