package predictions.dapp.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import predictions.dapp.controller.FootballDataController;
import predictions.dapp.service.FootballDataService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class FootballDataControllerUnitTest {

    @Mock
    private FootballDataService footballDataService;

    @InjectMocks
    private FootballDataController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Tag("unit")
    @Test
    void testGetCompetitions_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.putArray("competitions");

        Mockito.doReturn(mockResponse)
                .when(footballDataService)
                .getCompetitions();

        mockMvc.perform(get("/api/football/competitions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competitions").exists());
    }

    @Tag("unit")
    @Test
    void testGetTeams_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("name", "Boca Juniors");
        mockResponse.put("founded", 1905);

        Mockito.doReturn(mockResponse)
                .when(footballDataService)
                .getTeams();

        mockMvc.perform(get("/api/football/teams")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.founded").exists());
    }

    @Tag("unit")
    @Test
    void testGetTeamResults_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.putObject("filters");
        mockResponse.putObject("resultSet");
        mockResponse.putArray("matches");

        Mockito.doReturn(mockResponse)
                .when(footballDataService)
                .getResultsByTeam("2061");

        mockMvc.perform(get("/api/football/teams/2061/results")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) jsonPath("$.filters").exists())
                .andExpect((ResultMatcher) jsonPath("$.matches").exists());
    }


    @Tag("unit")
    @Test
    void testGetCompetitionMatches_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.putObject("filters");
        mockResponse.putArray("matches");

        Mockito.doReturn(mockResponse)
                .when(footballDataService)
                .getMatchesByCompetition("2003", 1);

        mockMvc.perform(get("/api/football/competitions/2003/matches")
                        .param("matchday", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters").exists())
                .andExpect(jsonPath("$.matches").exists());
    }

    @Tag("unit")
    @Test
    void testGetCompetitionResults_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.putObject("filters");
        mockResponse.putArray("matches");

        Mockito.doReturn(mockResponse)
                .when(footballDataService)
                .getResultsByCompetition("2003");

        mockMvc.perform(get("/api/football/competitions/2003/results")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters").exists())
                .andExpect(jsonPath("$.matches").exists());
    }

    @Tag("unit")
    @Test
    void testGetCompetitionFixtures_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.putObject("filters");
        mockResponse.putArray("matches");

        Mockito.doReturn(mockResponse)
                .when(footballDataService)
                .getFixtures("2152");

        mockMvc.perform(get("/api/football/competitions/2152/fixtures")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters").exists())
                .andExpect(jsonPath("$.matches").exists());
    }

    @Tag("unit")
    @Test
    void testGetTeamFixtures_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("name", "Sample Team");
        mockResponse.put("founded", 1905);

        Mockito.doReturn(mockResponse)
                .when(footballDataService)
                .getFixturesByTeam("2061");

        mockMvc.perform(get("/api/football/teams/2061/fixtures")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.founded").exists());
    }

    @Tag("unit")
    @Test
    void testGetTeamLastResult_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("result", "Last match result");

        Mockito.doReturn(mockResponse)
                .when(footballDataService)
                .getLastResultByTeam("2061");

        mockMvc.perform(get("/api/football/teams/2061/lastResult")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Last match result"));
    }

    @Tag("unit")
    @Test
    void testGetTeamFutureMatches_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("name", "Boca Juniors");
        mockResponse.put("founded", 1905);

        Mockito.doReturn(mockResponse)
                .when(footballDataService)
                .getFutureMatchesByTeamFromNowToEndOfYear("2061");

        mockMvc.perform(get("/api/football/teams/2061/futureMatches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.founded").exists());
    }

    @Tag("unit")
    @Test
    void testGetCompetitionMatches_InvalidId_Forbidden() throws Exception {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(footballDataService)
                .getMatchesByCompetition("9999", 1);

        mockMvc.perform(get("/api/football/competitions/9999/matches")
                        .param("matchday", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Tag("unit")
    @Test
    void testGetCompetitionResults_InvalidId_Forbidden() throws Exception {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(footballDataService)
                .getResultsByCompetition("9999");

        mockMvc.perform(get("/api/football/competitions/9999/results")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Tag("unit")
    @Test
    void testGetCompetitionFixtures_InvalidId_Forbidden() throws Exception {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(footballDataService)
                .getFixtures("9999");

        mockMvc.perform(get("/api/football/competitions/9999/fixtures")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Tag("unit")
    @Test
    void testGetTeamFixtures_InvalidId_Forbidden() throws Exception {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(footballDataService)
                .getFixturesByTeam("9999");

        mockMvc.perform(get("/api/football/teams/9999/fixtures")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Tag("unit")
    @Test
    void testGetTeamLastResult_InvalidId_Forbidden() throws Exception {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(footballDataService)
                .getLastResultByTeam("9999");

        mockMvc.perform(get("/api/football/teams/9999/lastResult")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Tag("unit")
    @Test
    void testGetTeamFutureMatches_InvalidId_Forbidden() throws Exception {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(footballDataService)
                .getFutureMatchesByTeamFromNowToEndOfYear("9999");

        mockMvc.perform(get("/api/football/teams/9999/futureMatches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}