package ru.practicum.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.practicum.blog.config.WebConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebAppConfiguration
@SpringJUnitConfig(classes = {
        IntegrationTestConfig.class,
        WebConfig.class
})
class CommentControllerIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    @Test
    void shouldCreateReadUpdateAndDeleteComment()
            throws Exception {

        // 1. Создаём пост для тестирования комментариев.
        String postResponse = mockMvc.perform(
                        post("/api/posts")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "title": "Test post",
                                          "text": "Test content",
                                          "tags": ["java"]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long postId = objectMapper
                .readTree(postResponse)
                .get("id")
                .asLong();

        // 2. POST: создаём комментарий.
        String createResponse = mockMvc.perform(
                        post("/api/posts/{postId}/comments", postId)
                                .contentType("application/json")
                                .content(
                                        "{\"text\":\"First comment\"," +
                                                "\"postId\":" + postId + "}"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.text").value("First comment")
                )
                .andExpect(
                        jsonPath("$.postId").value((int) postId)
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);

        long commentId = created.get("id").asLong();

        // Проверяем фактическое сохранение в H2.
        assertEquals(1, countComment(commentId, postId));
        assertEquals("First comment", commentText(commentId));

        // 3. GET: получаем список комментариев.
        mockMvc.perform(
                        get("/api/posts/{postId}/comments", postId)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].id").value((int) commentId)
                )
                .andExpect(
                        jsonPath("$[0].text").value("First comment")
                )
                .andExpect(
                        jsonPath("$[0].postId").value((int) postId)
                );

        // 4. PUT: редактируем комментарий.
        mockMvc.perform(
                        put(
                                "/api/posts/{postId}/comments/{commentId}",
                                postId,
                                commentId
                        )
                                .contentType("application/json")
                                .content(
                                        "{\"id\":" + commentId +
                                                ",\"text\":\"Updated comment\"," +
                                                "\"postId\":" + postId + "}"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.text").value("Updated comment")
                );

        assertEquals("Updated comment", commentText(commentId));

        // 5. GET: проверяем обновлённый комментарий.
        mockMvc.perform(
                        get(
                                "/api/posts/{postId}/comments/{commentId}",
                                postId,
                                commentId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id").value((int) commentId)
                )
                .andExpect(
                        jsonPath("$.text").value("Updated comment")
                )
                .andExpect(
                        jsonPath("$.postId").value((int) postId)
                );

        // 6. DELETE: удаляем комментарий.
        mockMvc.perform(
                        delete(
                                "/api/posts/{postId}/comments/{commentId}",
                                postId,
                                commentId
                        )
                )
                .andExpect(status().isOk());

        assertEquals(0, countComment(commentId, postId));

        // 7. GET: проверяем, что комментарий удалён.
        mockMvc.perform(
                        get("/api/posts/{postId}/comments", postId)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()").value(0)
                );

        mockMvc.perform(
                        get(
                                "/api/posts/{postId}/comments/{commentId}",
                                postId,
                                commentId
                        )
                )
                .andExpect(status().isNotFound());
    }

    private int countComment(long commentId, long postId) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments " +
                        "WHERE id = ? AND post_id = ?",
                Integer.class,
                commentId,
                postId
        );

        return count == null ? 0 : count;
    }

    private String commentText(long commentId) {

        return jdbcTemplate.queryForObject(
                "SELECT body FROM comments WHERE id = ?",
                String.class,
                commentId
        );
    }
}
