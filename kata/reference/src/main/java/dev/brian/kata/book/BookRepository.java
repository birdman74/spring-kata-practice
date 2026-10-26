package dev.brian.kata.book;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    List<Book> findByAuthorIgnoreCaseOrderByTitleAsc(String author);

    List<Book> findAllByOrderByTitleAsc();

    @Query("""
            select b from Book b
            where not exists (
                select l from Loan l where l.book = b and l.returnedAt is null)
            order by b.title asc
            """)
    List<Book> findAvailable();

    @Query("""
            select new dev.brian.kata.book.AuthorBookCount(b.author, count(b))
            from Book b
            group by b.author
            order by count(b) desc, b.author asc
            """)
    List<AuthorBookCount> countBooksByAuthor();
}
