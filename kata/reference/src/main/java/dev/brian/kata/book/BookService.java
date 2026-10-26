package dev.brian.kata.book;

import dev.brian.kata.error.ConflictException;
import dev.brian.kata.error.NotFoundException;
import dev.brian.kata.loan.LoanRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookService(BookRepository bookRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public BookResponse create(BookRequest bookRequest) {
        if (bookRepository.existsByIsbn(bookRequest.isbn())) {
            throw new ConflictException("Book with ISBN " + bookRequest.isbn() + " already exists");
        }
        Book book = new Book(bookRequest.isbn(), bookRequest.title(), bookRequest.author(),
                bookRequest.publishedYear());
        return BookResponse.from(bookRepository.save(book));
    }

    public BookResponse findById(long bookId) {
        return BookResponse.from(loadBook(bookId));
    }

    public List<BookResponse> list(String author) {
        List<Book> books = (author == null || author.isBlank())
                ? bookRepository.findAllByOrderByTitleAsc()
                : bookRepository.findByAuthorIgnoreCaseOrderByTitleAsc(author);
        return books.stream().map(BookResponse::from).toList();
    }

    @Transactional
    public BookResponse update(long bookId, BookUpdateRequest bookUpdateRequest) {
        Book book = loadBook(bookId);
        book.setTitle(bookUpdateRequest.title());
        book.setAuthor(bookUpdateRequest.author());
        book.setPublishedYear(bookUpdateRequest.publishedYear());
        return BookResponse.from(book);
    }

    @Transactional
    public void delete(long bookId) {
        Book book = loadBook(bookId);
        if (loanRepository.existsByBook(book)) {
            throw new ConflictException("Book " + bookId + " has loan history and cannot be deleted");
        }
        bookRepository.delete(book);
    }

    public PageResponse<BookResponse> page(Pageable pageable) {
        return PageResponse.from(bookRepository.findAll(pageable).map(BookResponse::from));
    }

    public List<BookResponse> available() {
        return bookRepository.findAvailable().stream().map(BookResponse::from).toList();
    }

    public List<AuthorBookCount> authorStats() {
        return bookRepository.countBooksByAuthor();
    }

    private Book loadBook(long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Book " + bookId + " not found"));
    }
}
