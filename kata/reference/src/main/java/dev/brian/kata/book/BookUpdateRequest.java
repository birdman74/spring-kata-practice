package dev.brian.kata.book;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookUpdateRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotNull @Min(1450) @Max(2100) Integer publishedYear) {
}
