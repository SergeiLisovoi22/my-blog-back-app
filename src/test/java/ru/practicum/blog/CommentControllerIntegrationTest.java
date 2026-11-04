package ru.practicum.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.WebApplicationContext;
import ru.practicum.blog.config.WebConfig;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebAppConfiguration
@SpringJUnitConfig(classes = {CommentControllerIntegrationTest.TestConfig.class, WebConfig.class})
class CommentControllerIntegrationTest {

    @Configuration
    @EnableTransactionManagement
    @ComponentScan(basePackages = {
            "ru.practicum.blog.repository",
            "ru.practicum.blog.service"
    })
    static class TestConfig {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .addScript("schema.sql")
                    .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void shouldCreateReadUpdateAndDeleteComment() throws Exception {
        String postResponse = mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content("{\"title\":\"Test post\",\"text\":\"Test content\",\"tags\":[\"java\"]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long postId = objectMapper.readTree(postResponse).get("id").asLong();

        String createResponse = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType("application/json")
                        .content("{\"text\":\"First comment\",\"postId\":" + postId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("First comment"))
                .andExpect(jsonPath("$.postId").value((int) postId))
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createResponse);
        long commentId = created.get("id").asLong();
        assertEquals(1, countComment(commentId, postId));
        assertEquals("First comment", commentText(commentId));

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) commentId))
                .andExpect(jsonPath("$[0].text").value("First comment"))
                .andExpect(jsonPath("$[0].postId").value((int) postId));

        mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                        .contentType("application/json")
                        .content("{\"id\":" + commentId + ",\"text\":\"Updated comment\",\"postId\":" + postId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated comment"));
        assertEquals("Updated comment", commentText(commentId));

        mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) commentId))
                .andExpect(jsonPath("$.text").value("Updated comment"))
                .andExpect(jsonPath("$.postId").value((int) postId));

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk());
        assertEquals(0, countComment(commentId, postId));

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isNotFound());
    }

    private int countComment(long commentId, long postId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE id = ? AND post_id = ?",
                Integer.class, commentId, postId);
        return count == null ? 0 : count;
    }

    private String commentText(long commentId) {
        return jdbcTemplate.queryForObject(
                "SELECT body FROM comments WHERE id = ?",
                String.class, commentId);
    }
}
