package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieTypeToken;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private static final int MAX_YEAR_FOR_MOVIE = 2027;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MIN_YEAR_FOR_MOVIE = 1888;
    private final Gson gson;
    public MoviesHandler(MoviesStore store) {
        this.store = store;
        this.gson = new GsonBuilder()
                .serializeNulls()
                .create();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        switch (method) {
            case "GET":
                String path = ex.getRequestURI().getPath();
                String[] parts = path.split("/");
                if (parts.length == 2) {
                    String query = ex.getRequestURI().getQuery();
                    if (query != null && query.contains("year")) {
                        getByYearHandler(ex, query);
                    }
                    if (parts[1].equals("movies")) {
                        getHandler(ex);
                    }
                } else if (parts.length == 3 && parts[1].equals("movies")) {
                    getByIdHandler(ex);
                }
                break;
            case "POST":
                postHandler(ex);
                break;
            case "DELETE":
                deleteHandler(ex);
                break;
            default:
                sendJson(ex, 405, "Method Not Allowed");
                break;
        }
    }

    private void getHandler(HttpExchange ex) throws IOException {
        if (store.isEmpty()) {
            sendJson(ex, 200, "[]");
        } else {
            Map<Integer, Movie> movies = store.getMovies();
            sendJson(ex, 200, gson.toJson(movies, new MovieTypeToken().getType()));
        }
    }

    private void postHandler(HttpExchange ex) throws IOException {
        List<String> contentTypes = ex.getRequestHeaders().get("Content-Type");
        if (contentTypes == null || contentTypes.isEmpty()) {
            sendJson(ex, 415, "Unsupported Media Type");
            return;
        }
        boolean isJson = contentTypes.stream()
                .anyMatch(ct -> ct.toLowerCase().trim().startsWith("application/json"));
        if (!isJson) {
            sendJson(ex, 415, "Unsupported Media Type");
            return;
        }
        try (InputStream in = ex.getRequestBody()) {
            Movie movie = checkAndValidateMovie(in, ex);
            if (movie == null) {
                return;
            }
            int id = store.addMovie(movie);
            JsonObject response = new JsonObject();
            response.addProperty("id", id);
            response.add("movie", gson.toJsonTree(movie));
            sendJson(ex, 201, gson.toJson(response));
        }
    }

    private void getByIdHandler(HttpExchange ex) throws IOException {
        Integer id = parseId(ex);
        if (id == null) {
            return;
        }
        Movie movie = store.getMovie(id);
        if (movie == null) {
            ErrorResponse errorResponse = new ErrorResponse("Not Found");
            errorResponse.addDescription("Фильм с id=" + id + " не найден");
            sendJson(ex, 404, gson.toJson(errorResponse));
            return;
        }
        sendJson(ex, 200, gson.toJson(movie));
    }

    private void deleteHandler(HttpExchange ex) throws IOException {
        Integer id = parseId(ex);
        if (id == null) {
            return;
        }
        if (store.removeMovie(id) != null) {
            sendNoContent(ex);
        } else {
            ErrorResponse errorResponse = new ErrorResponse("Not Found");
            errorResponse.addDescription("Фильм с id=" + id + " не найден");
            sendJson(ex, 404, gson.toJson(errorResponse));
        }
    }

    private void getByYearHandler(HttpExchange ex, String query) throws IOException {
        Integer year = parseYear(ex, query);
        if (year == null) {
            return;
        }
        Map<Integer, Movie> movies = new HashMap<>();
        store.getMovies().forEach((id, movie) -> {
            if (movie.getYear() == year) {
                movies.put(id, movie);
            }
        });
        sendJson(ex, 200, gson.toJson(movies, new MovieTypeToken().getType()));
    }

    public Integer parseYear(HttpExchange ex, String query) throws IOException {
        int year;
        String[] queryParts = query.split(";");
        String stringYear = "";
        for (String part : queryParts) {
            if (part.startsWith("year=")) {
                stringYear = part.substring(5);
                break;
            }
        }
        try {
            year = Integer.parseInt(stringYear);
        } catch (NumberFormatException e) {
            ErrorResponse errorResponse = new ErrorResponse("Bad Request");
            errorResponse.addDescription("Некорректный параметр запроса — 'year'");
            sendJson(ex, 400, gson.toJson(errorResponse));
            return null;
        }
        return year;
    }

    public Integer parseId(HttpExchange ex) throws IOException {
        int id;
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");
        String stringId = "";
        if (parts.length == 3 && parts[1].equals("movies")) {
            stringId = parts[2];
        }
        try {
            id = Integer.parseInt(stringId);
        } catch (NumberFormatException e) {
            ErrorResponse errorResponse = new ErrorResponse("Bad Request");
            errorResponse.addDescription("Некорректный ID");
            sendJson(ex, 400, gson.toJson(errorResponse));
            return null;
        }
        return id;
    }

    private Movie checkAndValidateMovie(InputStream in, HttpExchange ex) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации");
        try {
            byte[] bytes = in.readAllBytes();
            String body = new String(bytes, StandardCharsets.UTF_8);
            Movie movie = gson.fromJson(body, Movie.class);
            boolean hasErrors = false;
            if (movie.getTitle() == null || movie.getTitle().isEmpty()) {
                errorResponse.addDescription("Название фильма не может быть пустым");
                hasErrors = true;
            } else if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
                errorResponse.addDescription("Название фильма должно быть не более 100 символов");
                hasErrors = true;
            }
            if (movie.getYear() < MIN_YEAR_FOR_MOVIE || movie.getYear() > MAX_YEAR_FOR_MOVIE) {
                errorResponse.addDescription("Год фильма должен быть между 1888 и 2027");
                hasErrors = true;
            }
            if (hasErrors) {
                JsonObject response = new JsonObject();
                response.addProperty("error", errorResponse.getError());
                response.add("description", gson.toJsonTree(errorResponse.getDescription()));
                sendJson(ex, 422, gson.toJson(response));
                return null;
            }
            return movie;
        } catch (JsonSyntaxException e) {
            errorResponse.addDescription("Некорректный JSON");
            JsonObject response = new JsonObject();
            response.addProperty("error", errorResponse.getError());
            response.add("description", gson.toJsonTree(errorResponse.getDescription()));
            sendJson(ex, 422, gson.toJson(response));
            return null;
        }
    }
}