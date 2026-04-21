package ru.practicum.moviehub.http;

import com.google.gson.Gson;
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
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        store = new MoviesStore();
        server = new MoviesServer(store);
        server.start();
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
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
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
        Gson gson = new Gson();
        Movie movie = gson.fromJson(jsonMovie, Movie.class);
        JsonObject response = new JsonObject();
        response.addProperty("id", 1);
        response.add("movie", gson.toJsonTree(movie));
        String body = resp.body().trim();
        assertEquals(body, gson.toJson(response), "Ожидается JSON-массив c ID");
    }

    @Test
    void postMovie_whenDoesntHaveContentType_returns415() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"title\": \"Movie 1\", \"year\": 2002}";
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
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
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
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
        Gson gson = new Gson();
        JsonObject response = new JsonObject();
        response.addProperty("error", "Ошибка валидации");
        JsonArray descriptionArray = new JsonArray();
        descriptionArray.add("Название фильма должно быть не более 100 символов");
        response.add("description", descriptionArray);
        String body = resp.body().trim();
        assertEquals(body, gson.toJson(response), "Ожидается JSON-массив c ошибкой");
    }

    @Test
    void postMovie_whenTitleNull_returns422() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"year\": 2025}";
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
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
        Gson gson = new Gson();
        JsonObject response = new JsonObject();
        response.addProperty("error", "Ошибка валидации");
        JsonArray descriptionArray = new JsonArray();
        descriptionArray.add("Название фильма не может быть пустым");
        response.add("description", descriptionArray);
        String body = resp.body().trim();
        assertEquals(body, gson.toJson(response), "Ожидается JSON-массив c ошибкой");
    }

    @Test
    void postMovie_whenYear2028_returns422() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"title\": \"Movie 1\", \"year\": 2028}";
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
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
        Gson gson = new Gson();
        JsonObject response = new JsonObject();
        response.addProperty("error", "Ошибка валидации");
        JsonArray descriptionArray = new JsonArray();
        descriptionArray.add("Год фильма должен быть между 1888 и 2027");
        response.add("description", descriptionArray);
        String body = resp.body().trim();
        assertEquals(body, gson.toJson(response), "Ожидается JSON-массив c ошибкой");
    }

    @Test
    void postMovie_whenYear1887_returns422() throws Exception {
        Map<Integer, Movie> movies = store.getMovies();
        String jsonMovie = "{\"title\": \"Movie 1\", \"year\": 1887}";
        assertEquals(0, movies.size(), "Библиотека должна быть пустой");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
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
        Gson gson = new Gson();
        JsonObject response = new JsonObject();
        response.addProperty("error", "Ошибка валидации");
        JsonArray descriptionArray = new JsonArray();
        descriptionArray.add("Год фильма должен быть между 1888 и 2027");
        response.add("description", descriptionArray);
        String body = resp.body().trim();
        assertEquals(body, gson.toJson(response), "Ожидается JSON-массив c ошибкой");
    }
}