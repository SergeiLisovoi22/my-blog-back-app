package ru.practicum.blog.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.blog.model.*;
import ru.practicum.blog.repository.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class BlogService {
    private final PostRepository posts;
    private final CommentRepository comments;
    public BlogService(PostRepository posts, CommentRepository comments) { this.posts = posts; this.comments = comments; }

    @Transactional(readOnly = true)
    public PostPage list(String search, int pageNumber, int pageSize) {
        if (pageNumber < 1 || pageSize < 1 || pageSize > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        String titleQuery = Arrays.stream((search == null ? "" : search).trim().split("\\s+"))
                .filter(t -> !t.startsWith("#")).reduce("", (a, b) -> a.isEmpty() ? b : a + " " + b).toLowerCase(Locale.ROOT);
        List<String> tags = Arrays.stream((search == null ? "" : search).trim().split("\\s+"))
                .filter(t -> t.startsWith("#") && t.length() > 1).map(t -> t.substring(1)).toList();
        List<PostDto> filtered = posts.findAll().stream().filter(p -> p.title().toLowerCase(Locale.ROOT).contains(titleQuery))
                .filter(p -> p.tags().containsAll(tags)).toList();
        int total = filtered.size();
        int last = Math.max(1, (total + pageSize - 1) / pageSize);
        int from = (int) Math.min(total, ((long)pageNumber - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<PostDto> previews = filtered.subList(from, to).stream().map(p -> new PostDto(p.id(), p.title(),
                p.text().length() > 128 ? p.text().substring(0, 128) + "…" : p.text(), p.tags(), p.likesCount(), p.commentsCount())).toList();
        return new PostPage(previews, pageNumber > 1, pageNumber < last, last);
    }
    @Transactional(readOnly = true)
    public PostDto get(long id) { return requirePost(id); }
    private PostDto requirePost(long id) {
        PostDto p = posts.find(id);
        if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        return p;
    }
    private void validate(PostRequest request) {
        if (request == null || request.title() == null || request.title().isBlank() || request.text() == null || request.tags() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title, text and tags are required");
    }
    public PostDto create(PostRequest request) { validate(request); return requirePost(posts.insert(request)); }
    public PostDto update(long id, PostRequest request) {
        validate(request);
        if (posts.update(id, request) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return requirePost(id);
    }
    public void delete(long id) { if (posts.delete(id) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
    public int like(long id) { int result = posts.incrementLikes(id); if (result < 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND); return result; }
    public void saveImage(long id, byte[] bytes, String contentType) { requirePost(id); posts.saveImage(id, bytes, contentType); }
    @Transactional(readOnly = true)
    public byte[] image(long id) { requirePost(id); return posts.image(id); }
    @Transactional(readOnly = true)
    public String imageType(long id) { requirePost(id); return posts.imageType(id); }
    @Transactional(readOnly = true)
    public List<CommentDto> listComments(long id) { requirePost(id); return comments.list(id); }
    @Transactional(readOnly = true)
    public CommentDto getComment(long postId, long id) { requirePost(postId); return requireComment(postId, id); }
    private CommentDto requireComment(long postId, long id) {
        CommentDto c = comments.find(id);
        if (c == null || c.postId() != postId) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        return c;
    }
    public CommentDto addComment(long postId, CommentRequest request) {
        requirePost(postId);
        if (request == null || request.text() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return requireComment(postId, comments.insert(postId, request.text()));
    }
    public CommentDto updateComment(long postId, long id, CommentRequest request) {
        requireComment(postId, id);
        if (request == null || request.text() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        comments.update(id, postId, request.text());
        return requireComment(postId, id);
    }
    public void deleteComment(long postId, long id) {
        if (comments.delete(id, postId) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
