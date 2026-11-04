package ru.practicum.blog.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.practicum.blog.model.CommentDto;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Repository
public class CommentRepository {
    private final JdbcTemplate jdbc;
    public CommentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<CommentDto> list(long postId) {
        return jdbc.query("SELECT id, body, post_id FROM comments WHERE post_id = ? ORDER BY id", (rs, row) ->
                new CommentDto(rs.getLong("id"), rs.getString("body"), rs.getLong("post_id")), postId);
    }
    public CommentDto find(long commentId) {
        List<CommentDto> list = jdbc.query("SELECT id, body, post_id FROM comments WHERE id = ?", (rs, row) ->
                new CommentDto(rs.getLong("id"), rs.getString("body"), rs.getLong("post_id")), commentId);
        return list.isEmpty() ? null : list.getFirst();
    }
    public long insert(long postId, String text) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO comments(post_id, body) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, postId); ps.setString(2, text); return ps;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
    public int update(long id, long postId, String text) {
        return jdbc.update("UPDATE comments SET body = ? WHERE id = ? AND post_id = ?", text, id, postId);
    }
    public int delete(long id, long postId) {
        return jdbc.update("DELETE FROM comments WHERE id = ? AND post_id = ?", id, postId);
    }
}
