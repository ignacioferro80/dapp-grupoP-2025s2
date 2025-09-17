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

/**
 * Cliente simple para Football-Data v4 usando Java 11 HttpClient.
 * Devuelve JsonNode para evitar crear muchos POJOs.
 */
@Service
public class FootballDataService {

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String token; // resuelto por env o properties

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
        String path = "/competitions/" + competitionCode + "/matches";
        if (matchday != null) path += "?matchday=" + matchday;
        return get(path);
    }

    /** Resultados finalizados por competición */
    public JsonNode getResultsByCompetition(String competitionCode) throws IOException, InterruptedException {
        String path = "/competitions/" + competitionCode + "/matches?status=FINISHED";
        return get(path);
    }

    /** Próximos partidos (fixtures) por competición */
    public JsonNode getFixtures(String competitionCode) throws IOException, InterruptedException {
        String path = "/competitions/" + competitionCode + "/matches?status=SCHEDULED";
        return get(path);
    }

    /** Lista de equipos */
    public JsonNode getTeams() throws IOException, InterruptedException {
        return get("/teams");
    }

    /** Resultados finalizados por equipo */
    public JsonNode getResultsByTeam(String teamId) throws IOException, InterruptedException {
        String path = "/teams/" + teamId + "/matches?status=FINISHED";
        return get(path);
    }

    /** Próximos partidos (fixtures) por equipo */
    public JsonNode getFixturesByTeam(String teamId) throws IOException, InterruptedException {
        String path = "/teams/" + teamId + "/matches?status=SCHEDULED";
        return get(path);
    }

    public JsonNode getLastResultByTeam(String teamId) throws IOException, InterruptedException {
        String path = "/teams/" + teamId + "/matches?status=FINISHED&limit=1";
        return get(path);
    }


}
