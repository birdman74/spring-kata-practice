package dev.brian.kata.a02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.brian.kata.book.Book;
import dev.brian.kata.book.BookRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * A-02: the Book entity and its Spring Data repository, tested in a @DataJpaTest slice
 * (JPA only, no web layer, each test rolled back).
 */
@DataJpaTest
class A02BookRepositoryTest {

    private static final String EFFECTIVE_JAVA_ISBN = "9780134685991";

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void savingBookGeneratesIdAndPersistsAllFields() {
        Book savedBook = bookRepository.save(
                new Book(EFFECTIVE_JAVA_ISBN, "Effective Java", "Joshua Bloch", 2018));
        assertThat(savedBook.getId()).isNotNull();

        testEntityManager.flush();
        testEntityManager.clear();

        Book reloadedBook = bookRepository.findById(savedBook.getId()).orElseThrow();
        assertThat(reloadedBook.getIsbn()).isEqualTo(EFFECTIVE_JAVA_ISBN);
        assertThat(reloadedBook.getTitle()).isEqualTo("Effective Java");
        assertThat(reloadedBook.getAuthor()).isEqualTo("Joshua Bloch");
        assertThat(reloadedBook.getPublishedYear()).isEqualTo(2018);
    }

    @Test
    void findByIsbnReturnsMatchingBookOrEmpty() {
        bookRepository.save(new Book(EFFECTIVE_JAVA_ISBN, "Effective Java", "Joshua Bloch", 2018));

        Optional<Book> foundBook = bookRepository.findByIsbn(EFFECTIVE_JAVA_ISBN);
        Optional<Book> missingBook = bookRepository.findByIsbn("9999999999999");

        assertThat(foundBook).map(Book::getTitle).contains("Effective Java");
        assertThat(missingBook).isEmpty();
    }

    @Test
    void existsByIsbnReportsPresence() {
        bookRepository.save(new Book(EFFECTIVE_JAVA_ISBN, "Effective Java", "Joshua Bloch", 2018));

        assertThat(bookRepository.existsByIsbn(EFFECTIVE_JAVA_ISBN)).isTrue();
        assertThat(bookRepository.existsByIsbn("9999999999999")).isFalse();
    }

    @Test
    void findByAuthorIgnoresCaseAndOrdersByTitle() {
        bookRepository.save(new Book("9780321336781", "Java Puzzlers", "Joshua Bloch", 2005));
        bookRepository.save(new Book(EFFECTIVE_JAVA_ISBN, "Effective Java", "Joshua Bloch", 2018));
        bookRepository.save(new Book("9780132350884", "Clean Code", "Robert Martin", 2008));

        List<Book> blochBooks = bookRepository.findByAuthorIgnoreCaseOrderByTitleAsc("joshua bloch");

        assertThat(blochBooks).extracting(Book::getTitle)
                .containsExactly("Effective Java", "Java Puzzlers");
    }

    @Test
    void isbnMustBeUnique() {
        bookRepository.saveAndFlush(new Book(EFFECTIVE_JAVA_ISBN, "Effective Java", "Joshua Bloch", 2018));

        assertThatThrownBy(() -> bookRepository.saveAndFlush(
                new Book(EFFECTIVE_JAVA_ISBN, "Duplicate", "Someone Else", 2020)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void isbnIsRequired() {
        assertThatThrownBy(() -> bookRepository.saveAndFlush(
                new Book(null, "No Isbn", "Nobody", 2020)))
                .isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    @Test
    void changedFieldsArePersistedOnFlush() {
        Book savedBook = bookRepository.save(
                new Book(EFFECTIVE_JAVA_ISBN, "Effective Java", "Joshua Bloch", 2018));
        savedBook.setTitle("Effective Java, Third Edition");
        testEntityManager.flush();
        testEntityManager.clear();

        Book reloadedBook = bookRepository.findById(savedBook.getId()).orElseThrow();
        assertThat(reloadedBook.getTitle()).isEqualTo("Effective Java, Third Edition");
    }
}
