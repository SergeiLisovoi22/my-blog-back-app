package ru.practicum.blog.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.blog.model.*;
import ru.practicum.blog.service.BlogService;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final BlogService service;
    public PostController(BlogService service) { this.service = service; }

    @GetMapping
    public PostPage list(@RequestParam String search, @RequestParam int pageNumber, @RequestParam int pageSize) {
        return service.list(search, pageNumber, pageSize);
    }
    @GetMapping("/{id}")
    public PostDto get(@PathVariable long id) { return service.get(id); }

    @PostMapping("/{id}")
    public PostDto getPostAlias(@PathVariable long id) { return service.get(id); }
    @PostMapping
    public PostDto create(@RequestBody PostRequest request) { return service.create(request); }
    @PutMapping("/{id}")
    public PostDto update(@PathVariable long id, @RequestBody PostRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) { service.delete(id); return ResponseEntity.ok().build(); }
    @PostMapping("/{id}/likes")
    public int like(@PathVariable long id) { return service.like(id); }

    @PutMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveImage(@PathVariable long id, @RequestParam("image") MultipartFile image) throws IOException {
        if (image.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty image");
        service.saveImage(id, image.getBytes(), image.getContentType());
        return ResponseEntity.ok().build();
    }
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable long id) {
        byte[] bytes = service.image(id);
        if (bytes == null) return ResponseEntity.notFound().build();
        String type = service.imageType(id);
        MediaType mediaType;
        try { mediaType = type == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(type); }
        catch (Exception ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok().contentType(mediaType).body(bytes);
    }
    @GetMapping("/{postId}/comments")
    public List<CommentDto> comments(@PathVariable long postId) { return service.listComments(postId); }
    @GetMapping("/{postId}/comments/{commentId}")
    public CommentDto comment(@PathVariable long postId, @PathVariable long commentId) { return service.getComment(postId, commentId); }
    @PostMapping("/{postId}/comments")
    public CommentDto addComment(@PathVariable long postId, @RequestBody CommentRequest request) { return service.addComment(postId, request); }
    @PutMapping("/{postId}/comments/{commentId}")
    public CommentDto updateComment(@PathVariable long postId, @PathVariable long commentId, @RequestBody CommentRequest request) {
        return service.updateComment(postId, commentId, request);
    }
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable long postId, @PathVariable long commentId) {
        service.deleteComment(postId, commentId); return ResponseEntity.ok().build();
    }
}
