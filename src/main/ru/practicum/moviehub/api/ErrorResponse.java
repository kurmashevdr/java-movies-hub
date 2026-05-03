package ru.practicum.moviehub.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return Objects.equals(error, that.error) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        int result = error.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }
}