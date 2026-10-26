package dev.brian.kata.loan;

import java.time.Instant;

public record LoanResponse(Long id, Long bookId, Long memberId, Instant loanedAt, Instant returnedAt) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getBook().getId(), loan.getMember().getId(),
                loan.getLoanedAt(), loan.getReturnedAt());
    }
}
