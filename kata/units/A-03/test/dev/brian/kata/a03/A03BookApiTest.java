package dev.brian.kata.a03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.brian.kata.support.DatabaseCleaner;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * A-03: CRUD over HTTP for books, through a controller and a service.
 * Runs as a mock librarian so it keeps passing after A-07 locks the API down.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "librarian", roles = "LIBRARIAN")
class A03BookApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(dataSource).clean();
    }

    @Test
    void createReturns201WithLocationHeaderAndBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780134685991", "Effective Java", "Joshua Bloch", 2018)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.isbn").value("9780134685991"))
                .andExpect(jsonPath("$.title").value("Effective Java"))
                .andExpect(jsonPath("$.author").value("Joshua Bloch"))
                .andExpect(jsonPath("$.publishedYear").value(2018))
                .andReturn();

        long bookId = idOf(result);
        assertThat(result.getResponse().getHeader("Location")).endsWith("/api/books/" + bookId);
    }

    @Test
    void getByIdReturnsBook() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java", "Joshua Bloch", 2018);

        mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId))
                .andExpect(jsonPath("$.title").value("Effective Java"));
    }

    @Test
    void getUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/books/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void listAllReturnsBooksOrderedByTitle() throws Exception {
        createBook("9780321336781", "Java Puzzlers", "Joshua Bloch", 2005);
        createBook("9780132350884", "Clean Code", "Robert Martin", 2008);
        createBook("9780134685991", "Effective Java", "Joshua Bloch", 2018);

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Clean Code"))
                .andExpect(jsonPath("$[1].title").value("Effective Java"))
                .andExpect(jsonPath("$[2].title").value("Java Puzzlers"));
    }

    @Test
    void listByAuthorFiltersCaseInsensitively() throws Exception {
        createBook("9780321336781", "Java Puzzlers", "Joshua Bloch", 2005);
        createBook("9780132350884", "Clean Code", "Robert Martin", 2008);
        createBook("9780134685991", "Effective Java", "Joshua Bloch", 2018);

        mockMvc.perform(get("/api/books").param("author", "joshua BLOCH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Effective Java"))
                .andExpect(jsonPath("$[1].title").value("Java Puzzlers"));
    }

    @Test
    void updateChangesTitleAuthorAndYearButNotIsbn() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java", "Joshua Bloch", 2008);

        mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Effective Java, Third Edition",
                                 "author": "J. Bloch",
                                 "publishedYear": 2018}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId))
                .andExpect(jsonPath("$.isbn").value("9780134685991"))
                .andExpect(jsonPath("$.title").value("Effective Java, Third Edition"))
                .andExpect(jsonPath("$.author").value("J. Bloch"))
                .andExpect(jsonPath("$.publishedYear").value(2018));

        mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(jsonPath("$.title").value("Effective Java, Third Edition"));
    }

    @Test
    void updateUnknownIdReturns404() throws Exception {
        mockMvc.perform(put("/api/books/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Ghost", "author": "Nobody", "publishedYear": 2000}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204AndBookIsGone() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java", "Joshua Bloch", 2018);

        mockMvc.perform(delete("/api/books/{id}", bookId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUnknownIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/books/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    private long createBook(String isbn, String title, String author, int publishedYear) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(isbn, title, author, publishedYear)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private static long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private static String bookJson(String isbn, String title, String author, int publishedYear) {
        return """
                {"isbn": "%s", "title": "%s", "author": "%s", "publishedYear": %d}
                """.formatted(isbn, title, author, publishedYear);
    }
}
