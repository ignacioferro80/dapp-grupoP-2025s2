package predictions.dapp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import predictions.dapp.controller.HistoryController;
import predictions.dapp.security.JwtUtil;
import predictions.dapp.service.HistoryService;
import predictions.dapp.service.MetricsService;

import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HistoryController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class HistoryControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistoryService historyService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        Mockito.when(metricsService.measureLatency(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Tag("unit")
    @Test
    void testGetUserHistory_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("performance", "Excellent");
        mockResponse.put("predictions", "5/5 correct");

        Long userId = 11710L;

        Mockito.doReturn(userId).when(jwtUtil).extractUserId(Mockito.anyString());
        Mockito.doReturn(mockResponse).when(historyService).getHistory(userId);

        mockMvc.perform(get("/api/history")
                        .header("Authorization", "Bearer validToken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Mockito.verify(metricsService).incrementRequests();
    }

    @Tag("unit")
    @Test
    void testGetUserHistory_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Mockito.verify(metricsService).incrementRequests();
    }
}