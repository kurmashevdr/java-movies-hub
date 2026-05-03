package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetHandlerTest {
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
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        List<?> array = gson.fromJson(resp.body(), List.class);
        assertTrue(array.isEmpty(), "Ожидается пустой JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsArray() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Map<Integer, Movie> expectedMovies = store.getMovies();
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        Map<Integer, Movie> actualMovies = gson.fromJson(resp.body(), new MovieTypeToken().getType());
        assertEquals(expectedMovies, actualMovies, "Ожидается JSON-массив с фильмами");
    }

    @Test
    void getMovieById_whenMovieExists_returnsMovie() throws Exception {
        Movie movie = new Movie("Movie 2", 2003);
        store.addMovie("Movie 1", 2002);
        store.addMovie(movie);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/2"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        Movie actualMovie = gson.fromJson(resp.body(), Movie.class);
        assertEquals(movie, actualMovie, "Ожидается фильм с id=2");
    }

    @Test
    void getMovieById_whenMovieNotExists_returns404() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/4"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse expectedError = new ErrorResponse("Not Found");
        expectedError.addDescription("Фильм с id=4 не найден");
        assertEquals(404, resp.statusCode(), "GET /movies должен вернуть 404");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        ErrorResponse actualError = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals(expectedError, actualError, "Ожидается ошибка Not Found");
    }

    @Test
    void getMovieById_whenIdNonCorrect_returns400() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/asd"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse expectedError = new ErrorResponse("Bad Request");
        expectedError.addDescription("Некорректный ID");
        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        ErrorResponse actualError = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals(expectedError, actualError, "Ожидается ошибка Bad Request");
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
                .uri(URI.create(BASE + "?year=2002"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        Map<Integer, Movie> expected = new HashMap<>();
        expected.put(id1, movie1);
        expected.put(id4, movie4);
        Map<Integer, Movie> actual = gson.fromJson(resp.body(), new MovieTypeToken().getType());
        assertEquals(expected, actual, "Ожидается JSON-массив c фильмами 2002 года");
    }

    @Test
    void getMoviesByYear_whenYearNonCorrect_returns400() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "?year=asd"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        ErrorResponse expectedError = new ErrorResponse("Bad Request");
        expectedError.addDescription("Некорректный параметр запроса — 'year'");
        ErrorResponse actualError = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals(expectedError, actualError, "Ожидается ошибка Bad Request");
    }
}