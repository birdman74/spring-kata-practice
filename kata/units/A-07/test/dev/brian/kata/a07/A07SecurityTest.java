package dev.brian.kata.a07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * A-07: real authentication (HTTP Basic against your own users) and role-based authorization.
 * No mock users here: every request is anonymous unless it sends credentials.
 *
 * Users: librarian / librarian-pass (role LIBRARIAN), member / member-pass (role MEMBER).
 * 401 and 403 responses use the A-04 error format.
 */
@SpringBootTest
@AutoConfigureMockMvc
class A07SecurityTest {

    private static final RequestPostProcessor LIBRARIAN = httpBasic("librarian", "librarian-pass");
    private static final RequestPostProcessor MEMBER = httpBasic("member", "member-pass");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(dataSource).clean();
    }

    @Test
    void publicEndpointsNeedNoCredentials() throws Exception {
        mockMvc.perform(get("/api/ping")).andExpect(status().isOk());
        mockMvc.perform(get("/api/greeting")).andExpect(status().isOk());
        mockMvc.perform(get("/api/books")).andExpect(status().isOk());
        mockMvc.perform(get("/api/books/paged")).andExpect(status().isOk());
        mockMvc.perform(get("/api/books/available")).andExpect(status().isOk());
    }

    @Test
    void anonymousWriteReturns401InErrorFormat() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780134685991")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(httpBasic("librarian", "wrong-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780134685991")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void memberCannotManageBooksAndGets403InErrorFormat() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(MEMBER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780134685991")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        long bookId = createBookAsLibrarian("9780134685991");
        mockMvc.perform(put("/api/books/{id}", bookId)
                        .with(MEMBER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Hijacked", "author": "Mallory", "publishedYear": 2020}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/books/{id}", bookId).with(MEMBER))
                .andExpect(status().isForbidden());
    }

    @Test
    void librarianCanManageBooks() throws Exception {
        long bookId = createBookAsLibrarian("9780134685991");

        mockMvc.perform(delete("/api/books/{id}", bookId).with(LIBRARIAN))
                .andExpect(status().isNoContent());
    }

    @Test
    void onlyLibrarianCanManageMembers() throws Exception {
        mockMvc.perform(post("/api/members")
                        .with(MEMBER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson()))
                .andExpect(status().isForbidden());

        long memberId = createMemberAsLibrarian();

        mockMvc.perform(get("/api/members/{id}", memberId).with(MEMBER))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/members/{id}", memberId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loansRequireAuthenticationButEitherRoleMayBorrow() throws Exception {
        long bookId = createBookAsLibrarian("9780134685991");
        long memberId = createMemberAsLibrarian();
        String loanJson = """
                {"bookId": %d, "memberId": %d}
                """.formatted(bookId, memberId);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/loans")
                        .with(MEMBER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson))
                .andExpect(status().isCreated());
    }

    @Test
    void passwordsAreHashedWithBcrypt() {
        String encodedPassword = passwordEncoder.encode("librarian-pass");

        assertThat(encodedPassword).containsPattern("\\$2[aby]?\\$");
        assertThat(passwordEncoder.matches("librarian-pass", encodedPassword)).isTrue();
    }

    private long createBookAsLibrarian(String isbn) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .with(LIBRARIAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(isbn)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long createMemberAsLibrarian() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members")
                        .with(LIBRARIAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson()))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private static long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private static String bookJson(String isbn) {
        return """
                {"isbn": "%s", "title": "Effective Java", "author": "Joshua Bloch", "publishedYear": 2018}
                """.formatted(isbn);
    }

    private static String memberJson() {
        return """
                {"name": "Ada Lovelace", "email": "ada@example.com"}
                """;
    }
}
