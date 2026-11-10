
package ru.practicum.blog;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.blog.model.PostDto;
import ru.practicum.blog.model.PostPage;
import ru.practicum.blog.repository.CommentRepository;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.service.BlogService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlogServiceTest {

    @Test
    void rejectsInvalidPage() {
        PostRepository repository = mock(PostRepository.class);
        CommentRepository comments = mock(CommentRepository.class);

        BlogService service = new BlogService(repository, comments);

        assertThrows(
                ResponseStatusException.class,
                () -> service.list("", 0, 5)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void passesIndependentSearchWordsAndTagsToRepository() {
        PostRepository repository = mock(PostRepository.class);

        List<String> words = List.of("java", "spring");
        List<String> tags = List.of("backend");

        when(repository.countMatching(words, tags))
                .thenReturn(1);

        when(repository.findPage(words, tags, 5, 0L))
                .thenReturn(List.of(
                        new PostDto(
                                1L,
                                "Spring for Java developers",
                                "Text",
                                tags,
                                0,
                                0
                        )
                ));

        BlogService service = new BlogService(
                repository,
                mock(CommentRepository.class)
        );

        PostPage result = service.list(
                "JAVA   spring #backend",
                1,
                5
        );

        assertEquals(1, result.posts().size());
        assertEquals(1L, result.posts().getFirst().id());

        verify(repository).countMatching(words, tags);
        verify(repository).findPage(words, tags, 5, 0L);
    }

    @Test
    void searchSupportsTabsAndNewlines() {
        PostRepository repository = mock(PostRepository.class);

        List<String> words = List.of("java", "spring");
        List<String> tags = List.of("backend", "gradle");

        when(repository.countMatching(words, tags))
                .thenReturn(1);

        when(repository.findPage(words, tags, 5, 0L))
                .thenReturn(List.of(
                        new PostDto(
                                1L,
                                "Java Spring",
                                "Text",
                                tags,
                                0,
                                0
                        )
                ));

        BlogService service = new BlogService(
                repository,
                mock(CommentRepository.class)
        );

        PostPage result = service.list(
                "JAVA\tSpring\n#backend #gradle",
                1,
                5
        );

        assertEquals(1, result.posts().size());

        verify(repository).countMatching(words, tags);
        verify(repository).findPage(words, tags, 5, 0L);
    }

    @Test
    void calculatesOffsetAndTruncatesPreviews() {
        PostRepository repository = mock(PostRepository.class);

        when(repository.countMatching(List.of(), List.of()))
                .thenReturn(6);

        String longText = "x".repeat(150);

        when(repository.findPage(List.of(), List.of(), 5, 5L))
                .thenReturn(List.of(
                        new PostDto(
                                7L,
                                "Title",
                                longText,
                                List.of(),
                                1,
                                2
                        )
                ));

        BlogService service = new BlogService(
                repository,
                mock(CommentRepository.class)
        );

        PostPage result = service.list("", 2, 5);

        assertEquals(1, result.posts().size());
        assertEquals(129, result.posts().getFirst().text().length());

        assertTrue(result.hasPrev());
        assertFalse(result.hasNext());
        assertEquals(2, result.lastPage());

        verify(repository).findPage(
                List.of(),
                List.of(),
                5,
                5L
        );
    }
}
