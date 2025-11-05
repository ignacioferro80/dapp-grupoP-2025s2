package predictions.dapp.predictions;

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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;
import predictions.dapp.service.FootballDataService;
import predictions.dapp.service.PredictionService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class Predictions2Test {

    @Mock
    private FootballDataService footballDataService;

    @Mock
    private ConsultasRepository consultasRepository;

    @InjectMocks
    private PredictionService predictionService;

    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode createMockMatchesResponse() {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode matches = response.putArray("matches");

        for (int i = 0; i < 10; i++) {
            ObjectNode match = matches.addObject();
            match.put("id", 1000 + i);

            ObjectNode homeTeam = match.putObject("homeTeam");
            homeTeam.put("id", "86");
            homeTeam.put("name", "Real Madrid");

            ObjectNode awayTeam = match.putObject("awayTeam");
            awayTeam.put("id", "999");
            awayTeam.put("name", "Other Team");

            ObjectNode score = match.putObject("score");
            score.put("winner", i % 3 == 0 ? "HOME_TEAM" : (i % 3 == 1 ? "AWAY_TEAM" : "DRAW"));

            ObjectNode fullTime = score.putObject("fullTime");
            fullTime.put("home", i % 3 == 0 ? 3 : 1);
            fullTime.put("away", i % 3 == 1 ? 3 : 1);

            ObjectNode competition = match.putObject("competition");
            competition.put("name", "La Liga");
        }

        return response;
    }

    private ObjectNode createMockCompetitionsResponse() {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode competitions = response.putArray("competitions");

        ObjectNode comp = competitions.addObject();
        comp.put("id", "2014");
        comp.put("name", "La Liga");

        return response;
    }

    private ObjectNode createMockStandingsResponse() {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode standings = response.putArray("standings");
        ObjectNode standing = standings.addObject();
        ArrayNode table = standing.putArray("table");

        ObjectNode teamStanding = table.addObject();
        ObjectNode team = teamStanding.putObject("team");
        team.put("id", "86");
        team.put("name", "Real Madrid");
        teamStanding.put("position", 1);
        teamStanding.put("points", 75);
        teamStanding.put("goalDifference", 45);

        return response;
    }

    @BeforeEach
    void setUp() {
        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.of(new Consultas()));
        when(consultasRepository.save(any(Consultas.class))).thenAnswer(i -> i.getArguments()[0]);
    }



    @Tag("unit")
    @Test
    void testPredictWinner_NoCompetitionFound() throws IOException, InterruptedException {
        Long userId = 10L;

        ObjectNode emptyCompetitions = mapper.createObjectNode();
        emptyCompetitions.putArray("competitions");

        when(footballDataService.getLastMatchesFinished(anyString(), anyInt())).thenReturn(createMockMatchesResponse());
        when(footballDataService.getCompetitions()).thenReturn(emptyCompetitions);

        Map<String, Object> result = predictionService.predictWinner("86", "65", userId);

        assertNotNull(result);
        verify(footballDataService, never()).getStandings(anyString());
    }



    @Tag("unit")
    @Test
    void testPredictWinner_BothTeamsSameStats() throws IOException, InterruptedException {
        Long userId = 18L;

        ObjectNode identicalMatches = createMockMatchesResponse();

        when(footballDataService.getLastMatchesFinished(anyString(), anyInt()))
                .thenReturn(identicalMatches);
        when(footballDataService.getCompetitions())
                .thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getStandings(anyString()))
                .thenReturn(createMockStandingsResponse());

        Map<String, Object> result = predictionService.predictWinner("86", "86", userId);

        assertNotNull(result);
        assertTrue(result.containsKey("prediction"));
    }
}