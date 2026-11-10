package ru.practicum.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.practicum.blog.config.WebConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebAppConfiguration
@SpringJUnitConfig(classes = {
        IntegrationTestConfig.class,
        WebConfig.class
})
class BlogWebIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Test
    void createsPostAndFetchesItThroughMvcAndDatabase()
            throws Exception {

        MockMvc mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        // Создание поста
        String body = """
                {
                    "title": "Hello",
                    "text": "Hello Spring",
                    "tags": ["java"]
                }
                """;

        String response = mvc.perform(
                        post("/api/posts")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hello"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Получаем идентификатор созданного поста
        JsonNode json = new ObjectMapper().readTree(response);
        long id = json.get("id").asLong();

        // Проверяем получение поста через REST API
        mvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.text").value("Hello Spring")
                );
    }
}
