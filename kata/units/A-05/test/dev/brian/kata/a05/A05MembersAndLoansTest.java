package dev.brian.kata.a05;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * A-05: members, loans, entity relationships, and transactional business rules.
 * Rules: a book can have only one active loan; a member can have at most 3 active loans;
 * a loan can be returned once; a book with any loan history cannot be deleted.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "librarian", roles = "LIBRARIAN")
class A05MembersAndLoansTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(dataSource).clean();
    }

    // ---------- members ----------

    @Test
    void createMemberReturns201WithLocationAndBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson("Ada Lovelace", "ada@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andReturn();

        long memberId = idOf(result);
        assertThat(result.getResponse().getHeader("Location")).endsWith("/api/members/" + memberId);

        mockMvc.perform(get("/api/members/{id}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void memberValidationUsesFieldErrors() throws Exception {
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson("", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());
    }

    @Test
    void duplicateMemberEmailReturns409() throws Exception {
        createMember("Ada Lovelace", "ada@example.com");

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson("Another Ada", "ada@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void unknownMemberReturns404() throws Exception {
        mockMvc.perform(get("/api/members/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- loans ----------

    @Test
    void loanBookReturns201WithLoanBody() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java");
        long memberId = createMember("Ada Lovelace", "ada@example.com");

        loan(bookId, memberId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.bookId").value(bookId))
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.loanedAt").isNotEmpty());
    }

    @Test
    void loanRequestIsValidated() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.bookId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.memberId").isNotEmpty());
    }

    @Test
    void loanWithUnknownBookOrMemberReturns404() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java");
        long memberId = createMember("Ada Lovelace", "ada@example.com");

        loan(999999, memberId).andExpect(status().isNotFound());
        loan(bookId, 999999).andExpect(status().isNotFound());
    }

    @Test
    void bookAlreadyOnLoanCannotBeLoanedAgain() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java");
        long firstMemberId = createMember("Ada Lovelace", "ada@example.com");
        long secondMemberId = createMember("Grace Hopper", "grace@example.com");

        loan(bookId, firstMemberId).andExpect(status().isCreated());
        loan(bookId, secondMemberId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void memberCannotHoldMoreThanThreeActiveLoans() throws Exception {
        long memberId = createMember("Ada Lovelace", "ada@example.com");
        for (int i = 0; i < 3; i++) {
            long bookId = createBook("978000000000" + i, "Book " + i);
            loan(bookId, memberId).andExpect(status().isCreated());
        }
        long fourthBookId = createBook("9780000000009", "Book 9");

        loan(fourthBookId, memberId).andExpect(status().isConflict());
    }

    @Test
    void returningLoanSetsReturnedAtAndFreesTheBook() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java");
        long firstMemberId = createMember("Ada Lovelace", "ada@example.com");
        long secondMemberId = createMember("Grace Hopper", "grace@example.com");
        long loanId = idOf(loan(bookId, firstMemberId).andExpect(status().isCreated()).andReturn());

        mockMvc.perform(post("/api/loans/{id}/return", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());

        loan(bookId, secondMemberId).andExpect(status().isCreated());
    }

    @Test
    void returningTwiceReturns409AndUnknownLoanReturns404() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java");
        long memberId = createMember("Ada Lovelace", "ada@example.com");
        long loanId = idOf(loan(bookId, memberId).andExpect(status().isCreated()).andReturn());

        mockMvc.perform(post("/api/loans/{id}/return", loanId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/loans/{id}/return", loanId)).andExpect(status().isConflict());
        mockMvc.perform(post("/api/loans/{id}/return", 999999)).andExpect(status().isNotFound());
    }

    @Test
    void memberLoansCanBeFilteredToActiveOnly() throws Exception {
        long memberId = createMember("Ada Lovelace", "ada@example.com");
        long returnedBookId = createBook("9780134685991", "Effective Java");
        long activeBookId = createBook("9780132350884", "Clean Code");
        long returnedLoanId = idOf(loan(returnedBookId, memberId).andReturn());
        loan(activeBookId, memberId).andExpect(status().isCreated());
        mockMvc.perform(post("/api/loans/{id}/return", returnedLoanId)).andExpect(status().isOk());

        mockMvc.perform(get("/api/members/{id}/loans", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/members/{id}/loans", memberId).param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookId").value(activeBookId));
    }

    @Test
    void loansOfUnknownMemberReturns404() throws Exception {
        mockMvc.perform(get("/api/members/{id}/loans", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookWithLoanHistoryCannotBeDeleted() throws Exception {
        long bookId = createBook("9780134685991", "Effective Java");
        long memberId = createMember("Ada Lovelace", "ada@example.com");
        long loanId = idOf(loan(bookId, memberId).andReturn());
        mockMvc.perform(post("/api/loans/{id}/return", loanId)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/books/{id}", bookId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private ResultActions loan(long bookId, long memberId) throws Exception {
        return mockMvc.perform(post("/api/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bookId": %d, "memberId": %d}
                        """.formatted(bookId, memberId)));
    }

    private long createBook(String isbn, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "%s", "title": "%s", "author": "Kata Author", "publishedYear": 2020}
                                """.formatted(isbn, title)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long createMember(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(name, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private static long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private static String memberJson(String name, String email) {
        return """
                {"name": "%s", "email": "%s"}
                """.formatted(name, email);
    }
}
