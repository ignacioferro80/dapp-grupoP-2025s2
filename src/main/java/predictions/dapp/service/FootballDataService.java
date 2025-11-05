package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Cliente simple para Football-Data v4 usando Java 11 HttpClient.
 * Devuelve JsonNode para evitar crear muchos POJOs.
 */
@Service
public class FootballDataService {

    private static final String COMPETITIONS_PATH = "/competitions/";
    private static final String TEAMS_PATH = "/teams/";

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String token;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public FootballDataService(
            @Value("${football.api.base}") String baseUrl,
            @Value("${football.api.token:}") String tokenFromProps
    ) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        this.baseUrl = baseUrl;

        // Prioridad: variable de entorno
        String env = System.getenv("FOOTBALL_DATA_TOKEN");
        this.token = (env != null && !env.isBlank()) ? env : tokenFromProps;

        if (this.token == null || this.token.isBlank()) {
            throw new IllegalStateException(
                    "No se encontró token de Football-Data. Seteá FOOTBALL_DATA_TOKEN o 'football.api.token' en application.properties"
            );
        }
    }

    private JsonNode get(String pathAndQuery) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + pathAndQuery))
                .header("X-Auth-Token", token)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        int sc = res.statusCode();
        if (sc < 200 || sc >= 300) {
            throw new IOException("Football-Data HTTP " + sc + " body=" + res.body());
        }
        return mapper.readTree(res.body());
    }

    /** Lista de competiciones */
    public JsonNode getCompetitions() throws IOException, InterruptedException {
        return get("/competitions");
    }

    /** Partidos (fixtures/resultados) por competición; ej code "PL", "SA", etc. */
    public JsonNode getMatchesByCompetition(String competitionCode, Integer matchday) throws IOException, InterruptedException {
        String path = COMPETITIONS_PATH + competitionCode + "/matches";
        if (matchday != null) {
            path += "?matchday=" + matchday;
        }
        return get(path);
    }

    /** Resultados finalizados por competición */
    public JsonNode getResultsByCompetition(String competitionCode) throws IOException, InterruptedException {
        String path = COMPETITIONS_PATH + competitionCode + "/matches?status=FINISHED";
        return get(path);
    }

    /** Próximos partidos (fixtures) por competición */
    public JsonNode getFixtures(String competitionCode) throws IOException, InterruptedException {
        String path = COMPETITIONS_PATH + competitionCode + "/matches?status=SCHEDULED";
        return get(path);
    }

    /** Lista de equipos */
    public JsonNode getTeams() throws IOException, InterruptedException {
        return get("/teams");
    }

    /** Resultados finalizados por equipo */
    public JsonNode getResultsByTeam(String teamId) throws IOException, InterruptedException {
        String path = TEAMS_PATH + teamId + "/matches?status=FINISHED";
        return get(path);
    }

    /** Próximos partidos (fixtures) por equipo */
    public JsonNode getFixturesByTeam(String teamId) throws IOException, InterruptedException {
        String path = TEAMS_PATH + teamId + "/matches?status=SCHEDULED";
        return get(path);
    }

    public JsonNode getLastResultByTeam(String teamId) throws IOException, InterruptedException {
        String path = TEAMS_PATH + teamId + "/matches?status=FINISHED&limit=1";
        return get(path);
    }

    /**
     * Gets future matches for a team from today until the end of the year
     *
     * @param teamId teams id
     * @return JsonNode with future matches data
     * @throws IOException if theres an I/O error
     * @throws InterruptedException if the request is interrupted
     */
    public JsonNode getFutureMatchesByTeamFromNowToEndOfYear(String teamId) throws IOException, InterruptedException {
        LocalDate today = LocalDate.now();
        LocalDate endOfYear = LocalDate.of(today.getYear(), 12, 31);

        String dateFrom = today.format(DATE_FORMATTER);
        String dateTo = endOfYear.format(DATE_FORMATTER);

        String path = String.format("%s%s/matches?dateFrom=%s&dateTo=%s&status=SCHEDULED",
                TEAMS_PATH, teamId, dateFrom, dateTo);

        return get(path);
    }

    /**
     * Gets top scorers from a competition using competition ID
     *
     * @param competitionId ID of the competition (e.g., "2019" for Serie A)
     * @param limit Maximum number of scorers to return
     * @param season Season year (e.g., "2024")
     * @return JsonNode with top scorers data
     * @throws IOException si hay un error de I/O
     * @throws InterruptedException si la petición es interrumpida
     */
    public JsonNode getTopScorersByCompetitionId(String competitionId, int limit, String season) throws IOException, InterruptedException {
        String path = String.format("%s%s/scorers?limit=%d&season=%s",
                COMPETITIONS_PATH, competitionId, limit, season);
        return get(path);
    }

    /**
     * Gets player information by ID
     *
     * @param playerId ID of the player
     * @return JsonNode with player information
     * @throws IOException si hay un error de I/O
     * @throws InterruptedException si la petición es interrumpida
     */
    public JsonNode getPlayerById(String playerId) throws IOException, InterruptedException {
        String path = "/persons/" + playerId;
        return get(path);
    }

    //#################### PREDICTION METHODS #########################

    /**
     * Obtiene los últimos N partidos finalizados de un equipo
     *
     * @param teamId ID del equipo
     * @param limit Número de partidos a obtener
     * @return JsonNode con los partidos
     */
    public JsonNode getLastMatchesFinished(String teamId, int limit) throws IOException, InterruptedException {
        String path = String.format("%s%s/matches?status=FINISHED&limit=%d", TEAMS_PATH, teamId, limit);
        return get(path);
    }

    /**
     * Obtiene los standings de una competición
     *
     * @param competitionId ID de la competición
     * @return JsonNode con los standings
     */
    public JsonNode getStandings(String competitionId) throws IOException, InterruptedException {
        String path = String.format("%s%s/standings", COMPETITIONS_PATH, competitionId);
        return get(path);
    }
}