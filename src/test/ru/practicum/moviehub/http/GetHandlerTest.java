package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieTypeToken;
import ru.practicum.moviehub.store.MoviesStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetHandlerTest {
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
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsArray() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Map<Integer, Movie> movies = store.getMovies();
        Gson gson = new Gson();
        String json = gson.toJson(movies, new MovieTypeToken().getType());
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertEquals(body, json, "Ожидается JSON-массив");
    }

    @Test
    void getMovieById_whenMovieExists_returnsMovie() throws Exception {
        Movie movie = new Movie("Movie 2", 2003);
        store.addMovie("Movie 1", 2002);
        store.addMovie(movie);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Gson gson = new Gson();
        String json = gson.toJson(movie);
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertEquals(body, json, "Ожидается JSON-массив");
    }

    @Test
    void getMovieById_whenMovieNotExists_returns404() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/4"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Gson gson = new Gson();
        ErrorResponse errorResponse = new ErrorResponse("Not Found");
        errorResponse.addDescription("Фильм с id=4 не найден");
        String json = gson.toJson(errorResponse);
        assertEquals(404, resp.statusCode(), "GET /movies должен вернуть 404");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertEquals(body, json, "Ожидается JSON-массив c ошибкой");
    }

    @Test
    void getMovieById_whenIdNonCorrect_returns400() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/asd"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Gson gson = new Gson();
        ErrorResponse errorResponse = new ErrorResponse("Bad Request");
        errorResponse.addDescription("Некорректный ID");
        String json = gson.toJson(errorResponse);
        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertEquals(body, json, "Ожидается JSON-массив c ошибкой");
    }

    @Test
    void getMoviesByYear_whenYearCorrect_returns200() throws Exception {
        Movie movie1 = new Movie("Movie 1", 2002);
        Movie movie4 = new Movie("Movie 4", 2002);
        int id1 = store.addMovie(movie1);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        int id4 = store.addMovie(movie4);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2002"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Gson gson = new Gson();
        JsonObject response = new JsonObject();
        response.add(gson.toJson(id1), gson.toJsonTree(movie1));
        response.add(gson.toJson(id4), gson.toJsonTree(movie4));
        String json = gson.toJson(response);
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertEquals(body, json, "Ожидается JSON-массив c фильмами");
    }

    @Test
    void getMoviesByYear_whenYearNonCorrect_returns400() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=asd"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Gson gson = new Gson();
        ErrorResponse errorResponse = new ErrorResponse("Bad Request");
        errorResponse.addDescription("Некорректный параметр запроса — 'year'");
        String json = gson.toJson(errorResponse);
        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertEquals(body, json, "Ожидается JSON-массив c ошибкой");
    }
}