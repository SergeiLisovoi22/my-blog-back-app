
package ru.practicum.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateReadUpdateAndDeleteComment()
            throws Exception {

        // 1. Создаём тестовый пост.
        String postResponse = mockMvc.perform(
                        post("/api/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Test post",
                                            "text": "Test content",
                                            "tags": ["java"]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test post"))
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "text": "First comment",
                                            "postId": %d
                                        }
                                        """.formatted(postId))
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

        // Проверяем сохранение в H2.
        assertEquals(1, countComment(commentId, postId));
        assertEquals("First comment", commentText(commentId));

        // 3. GET: получаем комментарий из списка.
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

        // 4. PUT: изменяем комментарий.
        mockMvc.perform(
                        put(
                                "/api/posts/{postId}/comments/{commentId}",
                                postId,
                                commentId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "id": %d,
                                            "text": "Updated comment",
                                            "postId": %d
                                        }
                                        """.formatted(commentId, postId))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.text").value("Updated comment")
                );

        // Проверяем изменение данных в H2.
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

        // Проверяем удаление из H2.
        assertEquals(0, countComment(commentId, postId));

        // 7. GET: проверяем отсутствие комментариев.
        mockMvc.perform(
                        get("/api/posts/{postId}/comments", postId)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()").value(0)
                );

        // Удалённый комментарий должен возвращать 404.
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
                """
                SELECT COUNT(*)
                FROM comments
                WHERE id = ? AND post_id = ?
                """,
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
