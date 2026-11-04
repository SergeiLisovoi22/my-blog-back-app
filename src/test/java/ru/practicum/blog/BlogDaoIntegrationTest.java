package ru.practicum.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import ru.practicum.blog.model.*;
import ru.practicum.blog.repository.*;
import javax.sql.DataSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = BlogDaoIntegrationTest.TestConfig.class)
class BlogDaoIntegrationTest {
    @Configuration
    static class TestConfig {
        @Bean DataSource dataSource() { return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build(); }
        @Bean JdbcTemplate jdbcTemplate(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean PostRepository posts(JdbcTemplate jdbc) { return new PostRepository(jdbc); }
        @Bean CommentRepository comments(JdbcTemplate jdbc) { return new CommentRepository(jdbc); }
    }
    @Autowired PostRepository posts;
    @Autowired CommentRepository comments;
    @Test void storesPostTagsAndComments() {
        long id = posts.insert(new PostRequest(null, "Title", "Text", List.of("java", "spring")));
        assertEquals(List.of("java", "spring"), posts.find(id).tags());
        long commentId = comments.insert(id, "Hello");
        assertEquals(1, comments.list(id).size());
        assertEquals(1, posts.find(id).commentsCount());
        assertEquals(1, posts.incrementLikes(id));
        posts.delete(id);
        assertTrue(comments.list(id).isEmpty());
    }
}
