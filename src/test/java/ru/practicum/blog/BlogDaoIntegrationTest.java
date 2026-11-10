package ru.practicum.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.blog.config.WebConfig;
import ru.practicum.blog.model.PostDto;
import ru.practicum.blog.model.PostRequest;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.repository.CommentRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringJUnitConfig(classes = {
        IntegrationTestConfig.class,
        WebConfig.class
})
@Transactional
class BlogDaoIntegrationTest {

    @Autowired
    private PostRepository posts;

    @Autowired
    private CommentRepository comments;

    @Test
    void storesPostTagsAndComments() {

        long postId = posts.insert(
                new PostRequest(
                        null,
                        "Title",
                        "Text",
                        List.of("java", "spring")
                )
        );

        PostDto post = posts.find(postId);

        assertNotNull(post);
        assertEquals(
                List.of("java", "spring"),
                post.tags()
        );

        comments.insert(postId, "Hello");

        assertEquals(
                1,
                comments.list(postId).size()
        );

        assertEquals(
                1,
                posts.find(postId).commentsCount()
        );

        assertEquals(
                1,
                posts.incrementLikes(postId)
        );

        posts.delete(postId);

        assertTrue(
                comments.list(postId).isEmpty()
        );
    }

    @Test
    void feedIncludesAllTagsAndCommentCounts() {

        long first = posts.insert(
                new PostRequest(
                        null,
                        "First",
                        "Body",
                        List.of("java", "spring")
                )
        );

        long second = posts.insert(
                new PostRequest(
                        null,
                        "Second",
                        "Body",
                        List.of("gradle")
                )
        );

        comments.insert(first, "One");
        comments.insert(first, "Two");

        List<PostDto> feed = posts.findAll();

        PostDto firstPost = feed.stream()
                .filter(p -> p.id() == first)
                .findFirst()
                .orElseThrow();

        PostDto secondPost = feed.stream()
                .filter(p -> p.id() == second)
                .findFirst()
                .orElseThrow();

        assertEquals(
                List.of("java", "spring"),
                firstPost.tags()
        );

        assertEquals(
                2,
                firstPost.commentsCount()
        );

        assertEquals(
                List.of("gradle"),
                secondPost.tags()
        );

        assertEquals(
                0,
                secondPost.commentsCount()
        );
    }

    @Test
    void searchAndPaginationHappenInSql() {

        long first = posts.insert(
                new PostRequest(
                        null,
                        "Java and Spring",
                        "Body",
                        List.of("backend")
                )
        );

        posts.insert(
                new PostRequest(
                        null,
                        "Python only",
                        "Body",
                        List.of("backend")
                )
        );

        long second = posts.insert(
                new PostRequest(
                        null,
                        "Spring guide for Java",
                        "Body",
                        List.of("backend")
                )
        );

        List<String> words = List.of(
                "java",
                "spring"
        );

        List<String> tags = List.of("backend");

        // Проверяем поиск по отдельным словам и тегу.
        assertEquals(
                2,
                posts.countMatching(words, tags)
        );

        // Первая страница: самый новый подходящий пост.
        List<PostDto> firstPage = posts.findPage(
                words,
                tags,
                1,
                0L
        );

        assertEquals(1, firstPage.size());
        assertEquals(
                second,
                firstPage.getFirst().id()
        );

        // Вторая страница: следующий подходящий пост.
        List<PostDto> secondPage = posts.findPage(
                words,
                tags,
                1,
                1L
        );

        assertEquals(1, secondPage.size());
        assertEquals(
                first,
                secondPage.getFirst().id()
        );

        // Постов с отсутствующим тегом быть не должно.
        List<PostDto> emptyPage = posts.findPage(
                words,
                List.of("unknown"),
                5,
                0L
        );

        assertTrue(emptyPage.isEmpty());
    }
}
