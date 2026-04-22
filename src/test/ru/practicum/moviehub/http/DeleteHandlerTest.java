package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.store.MoviesStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeleteHandlerTest {
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
    void deleteMovieById_whenMovieExists_returns204() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/2"))
                .DELETE()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, resp.statusCode(), "DELETE /movies должен вернуть 204");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    @Test
    void deleteMovieById_whenMovieNotExists_returns404() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/5"))
                .DELETE()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode(), "DELETE /movies должен вернуть 404");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        ErrorResponse expectedError = new ErrorResponse("Not Found");
        expectedError.addDescription("Фильм с id=5 не найден");
        ErrorResponse actualError = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals(expectedError, actualError, "Ожидается ошибка Not Found");
    }

    @Test
    void deleteMovieById_whenIdNonCorrect_returns400() throws Exception {
        store.addMovie("Movie 1", 2002);
        store.addMovie("Movie 2", 2003);
        store.addMovie("Movie 3", 2004);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/asd"))
                .DELETE()
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "DELETE /movies должен вернуть 400");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        ErrorResponse expectedError = new ErrorResponse("Bad Request");
        expectedError.addDescription("Некорректный ID");
        ErrorResponse actualError = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals(expectedError, actualError, "Ожидается ошибка Bad Request");
    }
}
