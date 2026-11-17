
package ru.practicum.blog;

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
    void shouldCreateCommentAndPersistIt() throws Exception {
        long postId = createPost();

        long commentId = createComment(postId, "First comment");

        assertEquals(1, countComment(commentId, postId));
        assertEquals("First comment", commentText(commentId));
    }

    @Test
    void shouldGetCommentsForPost() throws Exception {
        long postId = createPost();
        long commentId = createComment(postId, "First comment");

        // Получаем список комментариев.
        mockMvc.perform(
                        get("/api/posts/{postId}/comments", postId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) commentId))
                .andExpect(jsonPath("$[0].text").value("First comment"))
                .andExpect(jsonPath("$[0].postId").value((int) postId));

        // Получаем отдельный комментарий.
        mockMvc.perform(
                        get("/api/posts/{postId}/comments/{commentId}",
                                postId, commentId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) commentId))
                .andExpect(jsonPath("$.text").value("First comment"))
                .andExpect(jsonPath("$.postId").value((int) postId));
    }

    @Test
    void shouldUpdateCommentAndPersistChanges() throws Exception {
        long postId = createPost();
        long commentId = createComment(postId, "First comment");

        // Редактируем комментарий.
        mockMvc.perform(
                        put("/api/posts/{postId}/comments/{commentId}",
                                postId, commentId)
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
                .andExpect(jsonPath("$.text").value("Updated comment"));

        // Проверяем изменения непосредственно в H2.
        assertEquals("Updated comment", commentText(commentId));

        // Проверяем обновлённый комментарий через REST API.
        mockMvc.perform(
                        get("/api/posts/{postId}/comments/{commentId}",
                                postId, commentId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated comment"));
    }

    @Test
    void shouldDeleteCommentAndReturnNotFound() throws Exception {
        long postId = createPost();
        long commentId = createComment(postId, "First comment");

        // Удаляем комментарий.
        mockMvc.perform(
                        delete("/api/posts/{postId}/comments/{commentId}",
                                postId, commentId)
                )
                .andExpect(status().isOk());

        // Проверяем удаление из базы данных.
        assertEquals(0, countComment(commentId, postId));

        // Проверяем, что список комментариев пуст.
        mockMvc.perform(
                        get("/api/posts/{postId}/comments", postId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Удалённый комментарий больше недоступен.
        mockMvc.perform(
                        get("/api/posts/{postId}/comments/{commentId}",
                                postId, commentId)
                )
                .andExpect(status().isNotFound());
    }

    // Общая подготовка тестового поста.
    private long createPost() throws Exception {
        String response = mockMvc.perform(
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
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    // Общая подготовка тестового комментария.
    private long createComment(long postId, String text)
            throws Exception {

        String requestBody = """
                {
                    "text": %s,
                    "postId": %d
                }
                """.formatted(
                objectMapper.writeValueAsString(text),
                postId
        );

        String response = mockMvc.perform(
                        post("/api/posts/{postId}/comments", postId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.text").value(text))
                .andExpect(jsonPath("$.postId").value((int) postId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
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
