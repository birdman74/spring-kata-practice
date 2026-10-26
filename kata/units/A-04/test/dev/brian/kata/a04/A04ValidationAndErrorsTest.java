package dev.brian.kata.a04;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
 * A-04: Bean Validation on request bodies and one consistent JSON error format for
 * every error, produced by a single @RestControllerAdvice.
 *
 * Error body: {"status": 404, "error": "Not Found", "message": "...", "fieldErrors": {...}}
 * fieldErrors is only required for validation failures.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "librarian", roles = "LIBRARIAN")
class A04ValidationAndErrorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(dataSource).clean();
    }

    @Test
    void blankFieldsAreRejectedWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "", "title": "  ", "author": "", "publishedYear": 2018}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.isbn").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.title").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.author").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.publishedYear").doesNotExist());
    }

    @Test
    void isbnMustBeExactlyThirteenDigits() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("12345", "Effective Java", "Joshua Bloch", 2018)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.isbn").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.title").doesNotExist());

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("978013468599X", "Effective Java", "Joshua Bloch", 2018)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.isbn").isNotEmpty());
    }

    @Test
    void publishedYearMustBeBetween1450And2100() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780134685991", "Effective Java", "Joshua Bloch", 1200)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.publishedYear").isNotEmpty());

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780134685991", "Effective Java", "Joshua Bloch", 2101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.publishedYear").isNotEmpty());
    }

    @Test
    void missingPublishedYearIsRejected() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "9780134685991", "title": "Effective Java", "author": "Joshua Bloch"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.publishedYear").isNotEmpty());
    }

    @Test
    void updateIsValidatedToo() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java", "Joshua Bloch", 2018);

        mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "author": "Joshua Bloch", "publishedYear": 2018}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").isNotEmpty());
    }

    @Test
    void notFoundUsesErrorFormatAndNamesTheId() throws Exception {
        mockMvc.perform(get("/api/books/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    @Test
    void duplicateIsbnReturns409AndNamesTheIsbn() throws Exception {
        createBook("9780134685991", "Effective Java", "Joshua Bloch", 2018);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780134685991", "Another Title", "Someone Else", 2020)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString("9780134685991")));
    }

    @Test
    void malformedJsonReturns400InErrorFormat() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    private long createBook(String isbn, String title, String author, int publishedYear) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(isbn, title, author, publishedYear)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private static String bookJson(String isbn, String title, String author, int publishedYear) {
        return """
                {"isbn": "%s", "title": "%s", "author": "%s", "publishedYear": %d}
                """.formatted(isbn, title, author, publishedYear);
    }
}
