package dev.brian.kata.book;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BookRequest(
        @NotBlank @Pattern(regexp = "\\d{13}", message = "must be exactly 13 digits") String isbn,
        @NotBlank String title,
        @NotBlank String author,
        @NotNull @Min(1450) @Max(2100) Integer publishedYear) {
}
