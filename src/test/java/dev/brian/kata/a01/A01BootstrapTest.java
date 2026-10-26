package dev.brian.kata.a01;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A-01: the application starts, serves JSON, and reads its greeting prefix from configuration.
 * All requests here are anonymous and must succeed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class A01BootstrapTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pingReturnsUpAsJson() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void greetingUsesConfiguredPrefixAndName() throws Exception {
        mockMvc.perform(get("/api/greeting").param("name", "Brian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Brian"));
    }

    @Test
    void greetingDefaultsNameToWorld() throws Exception {
        mockMvc.perform(get("/api/greeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, World"));
    }

    @Test
    void echoAcceptsAnonymousJsonPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "kata"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("kata"));
    }
}
