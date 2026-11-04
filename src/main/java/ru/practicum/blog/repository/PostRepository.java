package ru.practicum.blog.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.practicum.blog.model.PostDto;
import ru.practicum.blog.model.PostRequest;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class PostRepository {
    private final JdbcTemplate jdbc;
    public PostRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<PostDto> findAll() {
        return jdbc.query("SELECT id, title, body, likes_count FROM posts ORDER BY id DESC", (rs, row) -> findTagsAndBuild(
                rs.getLong("id"), rs.getString("title"), rs.getString("body"), rs.getInt("likes_count")));
    }
    public PostDto find(long id) {
        List<PostDto> result = jdbc.query("SELECT id, title, body, likes_count FROM posts WHERE id = ?", (rs, row) -> findTagsAndBuild(
                rs.getLong("id"), rs.getString("title"), rs.getString("body"), rs.getInt("likes_count")), id);
        return result.isEmpty() ? null : result.getFirst();
    }
    private PostDto findTagsAndBuild(long id, String title, String text, int likes) {
        List<String> tags = jdbc.queryForList("SELECT tag FROM post_tags WHERE post_id = ? ORDER BY tag", String.class, id);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE post_id = ?", Integer.class, id);
        return new PostDto(id, title, text, tags, likes, Objects.requireNonNull(count));
    }
    public long insert(PostRequest input) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO posts(title, body) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, input.title());
            ps.setString(2, input.text());
            return ps;
        }, keys);
        long id = Objects.requireNonNull(keys.getKey()).longValue();
        replaceTags(id, input.tags());
        return id;
    }
    public int update(long id, PostRequest input) {
        int updated = jdbc.update("UPDATE posts SET title = ?, body = ? WHERE id = ?", input.title(), input.text(), id);
        if (updated > 0) replaceTags(id, input.tags());
        return updated;
    }
    private void replaceTags(long id, List<String> tags) {
        jdbc.update("DELETE FROM post_tags WHERE post_id = ?", id);
        if (tags != null) tags.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct()
                .forEach(tag -> jdbc.update("INSERT INTO post_tags(post_id, tag) VALUES (?, ?)", id, tag));
    }
    public int delete(long id) { return jdbc.update("DELETE FROM posts WHERE id = ?", id); }
    public int incrementLikes(long id) {
        if (jdbc.update("UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?", id) == 0) return -1;
        return Objects.requireNonNull(jdbc.queryForObject("SELECT likes_count FROM posts WHERE id = ?", Integer.class, id));
    }
    public void saveImage(long id, byte[] bytes, String type) {
        jdbc.update("UPDATE posts SET image = ?, image_type = ? WHERE id = ?", bytes, type, id);
    }
    public byte[] image(long id) {
        List<byte[]> bytes = jdbc.query("SELECT image FROM posts WHERE id = ?", (rs, row) -> rs.getBytes(1), id);
        return bytes.isEmpty() ? null : bytes.getFirst();
    }
    public String imageType(long id) {
        List<String> types = jdbc.query("SELECT image_type FROM posts WHERE id = ?", (rs, row) -> rs.getString(1), id);
        return types.isEmpty() ? null : types.getFirst();
    }
}
