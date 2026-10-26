package dev.brian.kata.loan;

import jakarta.validation.constraints.NotNull;

public record LoanRequest(@NotNull Long bookId, @NotNull Long memberId) {
}
