package ru.practicum.blog;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.blog.model.*;
import ru.practicum.blog.repository.*;
import ru.practicum.blog.service.BlogService;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlogServiceTest {
    @Test void rejectsInvalidPage() {
        BlogService service = new BlogService(mock(PostRepository.class), mock(CommentRepository.class));
        assertThrows(ResponseStatusException.class, () -> service.list("", 0, 5));
    }
    @Test void searchSupportsTabsAndNewlines() {
        PostRepository repository = mock(PostRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                new PostDto(1, "Java Spring", "Text", List.of("spring", "gradle"), 0, 0),
                new PostDto(2, "Java Basics", "Text", List.of("java"), 0, 0)));
        BlogService service = new BlogService(repository, mock(CommentRepository.class));

        PostPage result = service.list("Java\t#spring\n#gradle", 1, 5);

        assertEquals(1, result.posts().size());
        assertEquals(1L, result.posts().getFirst().id());
    }

    @Test void searchMatchesTitleAndTags() {
        PostRepository repository = mock(PostRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                new PostDto(1, "Java Spring", "Text", List.of("java", "spring"), 0, 0),
                new PostDto(2, "Java Basics", "Text", List.of("java"), 0, 0)));
        BlogService service = new BlogService(repository, mock(CommentRepository.class));
        PostPage result = service.list("Java #spring", 1, 5);
        assertEquals(1, result.posts().size());
        assertEquals(1, result.posts().getFirst().id());
    }
}
