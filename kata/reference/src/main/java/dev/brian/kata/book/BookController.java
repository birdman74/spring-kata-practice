package dev.brian.kata.book;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest bookRequest) {
        BookResponse createdBook = bookService.create(bookRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBook.id())
                .toUri();
        return ResponseEntity.created(location).body(createdBook);
    }

    @GetMapping("/{id}")
    public BookResponse findById(@PathVariable("id") long bookId) {
        return bookService.findById(bookId);
    }

    @GetMapping
    public List<BookResponse> list(@RequestParam(name = "author", required = false) String author) {
        return bookService.list(author);
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable("id") long bookId,
                               @Valid @RequestBody BookUpdateRequest bookUpdateRequest) {
        return bookService.update(bookId, bookUpdateRequest);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long bookId) {
        bookService.delete(bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/paged")
    public PageResponse<BookResponse> page(@PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return bookService.page(pageable);
    }

    @GetMapping("/available")
    public List<BookResponse> available() {
        return bookService.available();
    }

    @GetMapping("/stats/authors")
    public List<AuthorBookCount> authorStats() {
        return bookService.authorStats();
    }
}
