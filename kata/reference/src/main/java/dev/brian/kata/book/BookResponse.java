package dev.brian.kata.book;

public record BookResponse(Long id, String isbn, String title, String author, int publishedYear) {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(),
                book.getAuthor(), book.getPublishedYear());
    }
}
