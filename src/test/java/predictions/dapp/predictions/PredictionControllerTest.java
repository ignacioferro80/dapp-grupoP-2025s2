package predictions.dapp.predictions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import predictions.dapp.controller.PredictionController;
import predictions.dapp.security.JwtUtil;
import predictions.dapp.service.MetricsService;
import predictions.dapp.service.PredictionService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PredictionControllerTest {

    @Mock
    private PredictionService predictionService;

    @Mock
    private MetricsService metricsService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private PredictionController predictionController;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        // Setup metricsService default behavior to pass through the callable
        doNothing().when(metricsService).incrementRequests();
        doNothing().when(metricsService).incrementErrors();
        when(metricsService.measureLatency(any())).thenAnswer(invocation -> {
            try {
                return invocation.getArgument(0, java.util.concurrent.Callable.class).call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setupAuthenticatedUser(String email, Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(jwtUtil.extractUserId(email)).thenReturn(userId);
    }




    @Tag("unit")
    @Test
    void testPredictMatchWinner_ServiceThrowsInterruptedException() throws Exception {
        Long userId = 1L;
        String email = "test@example.com";

        setupAuthenticatedUser(email, userId);
        when(predictionService.predictWinner(anyString(), anyString(), anyLong()))
                .thenThrow(new InterruptedException("Request interrupted"));

        ResponseEntity<Object> response = predictionController.predictMatchWinner("86", "65");

        assertEquals(500, response.getStatusCodeValue());
        verify(metricsService).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPredictMatchWinner_ServiceThrowsException() throws Exception {
        Long userId = 1L;
        String email = "test@example.com";

        setupAuthenticatedUser(email, userId);
        when(predictionService.predictWinner(anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("Service error"));

        ResponseEntity<Object> response = predictionController.predictMatchWinner("86", "65");

        assertEquals(500, response.getStatusCodeValue());
        verify(metricsService).incrementErrors();
    }






    @Tag("unit")
    @Test
    void testPredictMatchWinner_WithIOException() throws Exception {
        Long userId = 10L;
        String email = "user10@example.com";

        setupAuthenticatedUser(email, userId);
        when(predictionService.predictWinner(anyString(), anyString(), anyLong()))
                .thenThrow(new IOException("Network error"));

        ResponseEntity<Object> response = predictionController.predictMatchWinner("170", "180");

        assertEquals(500, response.getStatusCodeValue());
        verify(metricsService).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPredictMatchWinner_ServiceThrowsIllegalArgumentException() throws Exception {
        Long userId = 23L;
        String email = "user23@example.com";

        setupAuthenticatedUser(email, userId);
        when(predictionService.predictWinner(anyString(), anyString(), anyLong()))
                .thenThrow(new IllegalArgumentException("Invalid team IDs"));

        ResponseEntity<Object> response = predictionController.predictMatchWinner("700", "800");

        assertEquals(500, response.getStatusCodeValue());
        verify(metricsService).incrementErrors();
    }

    @Tag("unit")
    @Test
    void testPredictMatchWinner_ServiceThrowsNullPointerException() throws Exception {
        Long userId = 24L;
        String email = "user24@example.com";

        setupAuthenticatedUser(email, userId);
        when(predictionService.predictWinner(anyString(), anyString(), anyLong()))
                .thenThrow(new NullPointerException("Null data"));

        ResponseEntity<Object> response = predictionController.predictMatchWinner("900", "1000");

        assertEquals(500, response.getStatusCodeValue());
        verify(metricsService).incrementErrors();
    }










    @Tag("unit")
    @Test
    void testPredictMatchWinner_InterruptedExceptionHandling() throws Exception {
        Long userId = 32L;
        String email = "user32@example.com";

        setupAuthenticatedUser(email, userId);
        when(predictionService.predictWinner(anyString(), anyString(), anyLong()))
                .thenThrow(new InterruptedException("Thread interrupted"));

        ResponseEntity<Object> response = predictionController.predictMatchWinner("800", "900");

        assertEquals(500, response.getStatusCodeValue());
        verify(predictionService).predictWinner("800", "900", userId);
        verify(metricsService).incrementErrors();
    }



    @Tag("unit")
    @Test
    void testPredictMatchWinner_ErrorIncrements() throws Exception {
        Long userId = 34L;
        String email = "user34@example.com";

        setupAuthenticatedUser(email, userId);
        when(predictionService.predictWinner(anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("Error"));

        predictionController.predictMatchWinner("333", "444");

        verify(metricsService).incrementErrors();
    }


}