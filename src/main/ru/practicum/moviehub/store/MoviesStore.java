package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;
import java.util.HashMap;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int maxId = 0;

    public int addMovie(String title, int year) {
        int id = ++maxId;
        movies.put(id, new Movie(title, year));
        return id;
    }

    public int addMovie(Movie movie) {
        int id = ++maxId;
        movies.put(id, movie);
        return id;
    }

    public Movie getMovie(int id) {
        if (!movies.containsKey(id)) {
            return null;
        }
        return movies.get(id);
    }

    public boolean removeMovie(int id) {
        if (!movies.containsKey(id)) {
            return false;
        }
        movies.remove(id);
        return true;
    }

    public boolean isEmpty() {
        return movies.isEmpty();
    }

    public void clear() {
        maxId = 0;
        movies.clear();
    }

    public Map<Integer, Movie> getMovies() {
        return movies;
    }
}