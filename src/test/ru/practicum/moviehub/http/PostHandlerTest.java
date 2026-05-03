package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostHandlerTest {
    private static final String BASE = "http://localhost:8080/movies";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        store = new MoviesStore();
        server = new MoviesServer(store);
        server.start();
        gson = new GsonBuilder()
                .serializeNulls()
                .create();

    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void postMovie_whenValid_returns201() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"title\": \"Movie 1\", \"year\": 2002}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .headers("Content-Type", "application/json; charset=UTF-8")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertEquals(1, movies.size(), "Библиотека должна содержать 1 фильм");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        JsonObject response = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals(1, response.get("id").getAsInt(), "ID должен быть 1");
        Movie expectedMovie = gson.fromJson(jsonMovie, Movie.class);
        Movie actualMovie = gson.fromJson(response.get("movie"), Movie.class);
        assertEquals(expectedMovie, actualMovie, "Фильм в ответе должен соответствовать отправленному");
    }

    @Test
    void postMovie_whenDoesntHaveContentType_returns415() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"title\": \"Movie 1\", \"year\": 2002}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    @Test
    void postMovie_whenTitleLengthMore100_returns422() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"title\": \"" + "a".repeat(101) + "\", \"year\": 2025}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .headers("Content-Type", "application/json; charset=UTF-8")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        JsonObject actualResponse = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals("Ошибка валидации", actualResponse.get("error").getAsString());
        JsonArray description = actualResponse.getAsJsonArray("description");
        assertEquals(1, description.size());
        assertEquals("Название фильма должно быть не более 100 символов", description.get(0).getAsString());
    }

    @Test
    void postMovie_whenTitleNull_returns422() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"year\": 2025}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .headers("Content-Type", "application/json; charset=UTF-8")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        JsonObject actualResponse = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals("Ошибка валидации", actualResponse.get("error").getAsString());
        JsonArray description = actualResponse.getAsJsonArray("description");
        assertEquals(1, description.size());
        assertEquals("Название фильма не может быть пустым", description.get(0).getAsString());
    }

    @Test
    void postMovie_whenYear2028_returns422() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"title\": \"Movie 1\", \"year\": 2028}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .headers("Content-Type", "application/json; charset=UTF-8")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        JsonObject actualResponse = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals("Ошибка валидации", actualResponse.get("error").getAsString());
        JsonArray description = actualResponse.getAsJsonArray("description");
        assertEquals(1, description.size());
        assertEquals("Год фильма должен быть между 1888 и 2027", description.get(0).getAsString());
    }

    @Test
    void postMovie_whenYear1887_returns422() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"title\": \"Movie 1\", \"year\": 1887}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .headers("Content-Type", "application/json; charset=UTF-8")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        JsonObject actualResponse = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals("Ошибка валидации", actualResponse.get("error").getAsString());
        JsonArray description = actualResponse.getAsJsonArray("description");
        assertEquals(1, description.size());
        assertEquals("Год фильма должен быть между 1888 и 2027", description.get(0).getAsString());
    }

    @Test
    void methodNotAllowed_returns405() throws Exception {
        String jsonMovie = "{\"title\": \"Movie 1\", \"year\": 1887}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .headers("Content-Type", "application/json; charset=UTF-8")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(405, resp.statusCode(), "PUT /movies должен вернуть 405");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertEquals(body, "Method Not Allowed", "Ожидается JSON-массив c ошибкой");
    }
}