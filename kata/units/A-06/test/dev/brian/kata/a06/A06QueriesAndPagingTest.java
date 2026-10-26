package dev.brian.kata.a06;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.brian.kata.support.DatabaseCleaner;
import java.util.HashMap;
import java.util.Map;
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
 * A-06: Spring Data paging and sorting, a custom JPQL query, and a JPQL aggregate projection.
 *
 * Page body: {"content": [...books], "page": 0, "size": 20, "totalElements": 5, "totalPages": 1}
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "librarian", roles = "LIBRARIAN")
class A06QueriesAndPagingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private final Map<String, Long> bookIdsByTitle = new HashMap<>();

    @BeforeEach
    void seedCatalog() throws Exception {
        new DatabaseCleaner(dataSource).clean();
        bookIdsByTitle.clear();
        createBook("9780132350884", "Clean Code", "Robert Martin", 2008);
        createBook("9780321125217", "Domain-Driven Design", "Eric Evans", 2003);
        createBook("9780134685991", "Effective Java", "Joshua Bloch", 2018);
        createBook("9780321336781", "Java Puzzlers", "Joshua Bloch", 2005);
        createBook("9780134757599", "Refactoring", "Martin Fowler", 2018);
    }

    @Test
    void pagedDefaultsToFirstPageOfTwentySortedByTitle() throws Exception {
        mockMvc.perform(get("/api/books/paged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.content[4].title").value("Refactoring"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void pagedHonorsPageSizeAndSortParameters() throws Exception {
        mockMvc.perform(get("/api/books/paged")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Effective Java"))
                .andExpect(jsonPath("$.content[1].title").value("Java Puzzlers"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void pagedCanSortDescendingByAnotherField() throws Exception {
        mockMvc.perform(get("/api/books/paged")
                        .param("size", "5")
                        .param("sort", "publishedYear,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publishedYear").value(2018))
                .andExpect(jsonPath("$.content[4].title").value("Domain-Driven Design"));
    }

    @Test
    void availableListsOnlyBooksWithoutAnActiveLoanOrderedByTitle() throws Exception {
        long memberId = createMember("Ada Lovelace", "ada@example.com");
        loan(bookIdsByTitle.get("Effective Java"), memberId);
        long returnedLoanId = loan(bookIdsByTitle.get("Clean Code"), memberId);
        mockMvc.perform(post("/api/loans/{id}/return", returnedLoanId)).andExpect(status().isOk());

        mockMvc.perform(get("/api/books/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].title").value("Clean Code"))
                .andExpect(jsonPath("$[1].title").value("Domain-Driven Design"))
                .andExpect(jsonPath("$[2].title").value("Java Puzzlers"))
                .andExpect(jsonPath("$[3].title").value("Refactoring"));
    }

    @Test
    void authorStatsCountBooksPerAuthorMostFirstThenByName() throws Exception {
        mockMvc.perform(get("/api/books/stats/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].author").value("Joshua Bloch"))
                .andExpect(jsonPath("$[0].bookCount").value(2))
                .andExpect(jsonPath("$[1].author").value("Eric Evans"))
                .andExpect(jsonPath("$[1].bookCount").value(1))
                .andExpect(jsonPath("$[2].author").value("Martin Fowler"))
                .andExpect(jsonPath("$[3].author").value("Robert Martin"));
    }

    private void createBook(String isbn, String title, String author, int publishedYear) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "%s", "title": "%s", "author": "%s", "publishedYear": %d}
                                """.formatted(isbn, title, author, publishedYear)))
                .andExpect(status().isCreated())
                .andReturn();
        bookIdsByTitle.put(title, idOf(result));
    }

    private long createMember(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "email": "%s"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long loan(long bookId, long memberId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": %d, "memberId": %d}
                                """.formatted(bookId, memberId)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private static long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
