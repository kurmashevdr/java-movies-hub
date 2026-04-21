package ru.practicum.moviehub.api;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {
    private final String error;
    private final List<String> description;

    public ErrorResponse(String error) {
        this.error = error;
        this.description = new ArrayList<>();
    }

    public void addDescription(String description) {
        this.description.add(description);
    }

    public String getError() {
        return error;
    }

    public List<String> getDescription() {
        return description;
    }
}