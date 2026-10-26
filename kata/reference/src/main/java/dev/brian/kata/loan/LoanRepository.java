package dev.brian.kata.loan;

import dev.brian.kata.book.Book;
import dev.brian.kata.member.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    boolean existsByBook(Book book);

    boolean existsByBookAndReturnedAtIsNull(Book book);

    long countByMemberAndReturnedAtIsNull(Member member);

    List<Loan> findByMemberOrderByIdAsc(Member member);

    List<Loan> findByMemberAndReturnedAtIsNullOrderByIdAsc(Member member);
}
