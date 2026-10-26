package dev.brian.kata.a01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.brian.kata.config.KataProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A-01: the greeting prefix is bound through a type-safe @ConfigurationProperties record,
 * not hard-coded. This test overrides the property and expects the override to win.
 */
@SpringBootTest(properties = "kata.greeting=Howdy")
@AutoConfigureMockMvc
class A01ConfigurationPropertiesTest {

    @Autowired
    private KataProperties kataProperties;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void propertiesRecordIsBoundFromEnvironment() {
        assertEquals("Howdy", kataProperties.greeting());
    }

    @Test
    void greetingReflectsOverriddenProperty() throws Exception {
        mockMvc.perform(get("/api/greeting").param("name", "Brian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Howdy, Brian"));
    }
}
