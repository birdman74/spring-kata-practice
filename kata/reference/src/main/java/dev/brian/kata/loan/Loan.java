package dev.brian.kata.loan;

import dev.brian.kata.book.Book;
import dev.brian.kata.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Instant loanedAt;

    private Instant returnedAt;

    protected Loan() {
        // required by JPA
    }

    public Loan(Book book, Member member, Instant loanedAt) {
        this.book = book;
        this.member = member;
        this.loanedAt = loanedAt;
    }

    public boolean isActive() {
        return returnedAt == null;
    }

    public void markReturned(Instant returnTime) {
        this.returnedAt = returnTime;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Member getMember() {
        return member;
    }

    public Instant getLoanedAt() {
        return loanedAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }
}
