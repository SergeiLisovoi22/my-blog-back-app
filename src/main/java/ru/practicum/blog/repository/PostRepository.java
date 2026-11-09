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

    public PostRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Получаем посты, теги и количество комментариев
    // одним SQL-запросом вместо дополнительных запросов
    // для каждого поста.
    private static final String SELECT_POSTS = """
            SELECT p.id, p.title, p.body, p.likes_count,
                   t.tag,
                   COALESCE(cc.comments_count, 0) AS comments_count
            FROM posts p
            LEFT JOIN post_tags t ON t.post_id = p.id
            LEFT JOIN (
                SELECT post_id, COUNT(*) AS comments_count
                FROM comments
                GROUP BY post_id
            ) cc ON cc.post_id = p.id
            """;

    public List<PostDto> findAll() {
        return loadPosts(
                SELECT_POSTS + " ORDER BY p.id DESC, t.tag"
        );
    }

    // Подсчёт подходящих постов на стороне БД.
    public int countMatching(
            List<String> titleWords,
            List<String> tags) {

        SearchSql search = searchWhere(titleWords, tags);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posts p" + search.where(),
                Integer.class,
                search.arguments().toArray()
        );

        return Objects.requireNonNull(count);
    }

    // Поиск и пагинация выполняются в SQL.
    public List<PostDto> findPage(
            List<String> titleWords,
            List<String> tags,
            int pageSize,
            long offset) {

        SearchSql search = searchWhere(titleWords, tags);

        List<Object> args = new ArrayList<>(
                search.arguments()
        );

        args.add(pageSize);
        args.add(offset);

        String sql = """
                SELECT p.id, p.title, p.body, p.likes_count,
                       t.tag,
                       COALESCE(cc.comments_count, 0) AS comments_count
                FROM (
                    SELECT p.id, p.title, p.body, p.likes_count
                    FROM posts p
                """ + search.where() + """
                    ORDER BY p.id DESC
                    LIMIT ? OFFSET ?
                ) p
                LEFT JOIN post_tags t ON t.post_id = p.id
                LEFT JOIN (
                    SELECT post_id, COUNT(*) AS comments_count
                    FROM comments
                    GROUP BY post_id
                ) cc ON cc.post_id = p.id
                ORDER BY p.id DESC, t.tag
                """;

        return loadPosts(sql, args.toArray());
    }

    private record SearchSql(
            String where,
            List<Object> arguments) {
    }

    // Каждое слово и каждый тег — отдельное условие.
    private SearchSql searchWhere(
            List<String> titleWords,
            List<String> tags) {

        StringBuilder where = new StringBuilder(
                " WHERE 1 = 1"
        );

        List<Object> args = new ArrayList<>();

        for (String word : titleWords) {
            where.append(
                    " AND LOWER(p.title) LIKE ? ESCAPE '!'"
            );

            String escaped = word
                    .replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_");

            args.add("%" + escaped + "%");
        }

        for (String tag : tags) {
            where.append(
                    " AND EXISTS (" +
                            "SELECT 1 FROM post_tags filtered " +
                            "WHERE filtered.post_id = p.id " +
                            "AND filtered.tag = ?)"
            );

            args.add(tag);
        }

        return new SearchSql(where.toString(), args);
    }

    public PostDto find(long id) {
        List<PostDto> result = loadPosts(
                SELECT_POSTS +
                        " WHERE p.id = ? ORDER BY p.id DESC, t.tag",
                id
        );

        return result.isEmpty() ? null : result.getFirst();
    }

    // Собираем один PostDto из нескольких SQL-строк
    // (если у поста несколько тегов).
    private List<PostDto> loadPosts(
            String sql,
            Object... params) {

        return jdbc.query(sql, rs -> {

            java.util.Map<Long, PostRows> rows =
                    new java.util.LinkedHashMap<>();

            while (rs.next()) {
                long id = rs.getLong("id");

                PostRows post = rows.get(id);

                if (post == null) {
                    post = new PostRows(
                            rs.getString("title"),
                            rs.getString("body"),
                            rs.getInt("likes_count"),
                            rs.getInt("comments_count")
                    );

                    rows.put(id, post);
                }

                String tag = rs.getString("tag");

                if (tag != null) {
                    post.tags.add(tag);
                }
            }

            List<PostDto> result =
                    new ArrayList<>(rows.size());

            for (var entry : rows.entrySet()) {
                PostRows post = entry.getValue();

                result.add(new PostDto(
                        entry.getKey(),
                        post.title,
                        post.text,
                        post.tags,
                        post.likes,
                        post.commentCount
                ));
            }

            return result;
        }, params);
    }

    private static final class PostRows {

        final String title;
        final String text;
        final int likes;
        final int commentCount;

        final List<String> tags = new ArrayList<>();

        PostRows(
                String title,
                String text,
                int likes,
                int commentCount) {

            this.title = title;
            this.text = text;
            this.likes = likes;
            this.commentCount = commentCount;
        }
    }

    public long insert(PostRequest input) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO posts(title, body) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, input.title());
            ps.setString(2, input.text());

            return ps;
        }, keys);

        long id = Objects.requireNonNull(
                keys.getKey()
        ).longValue();

        replaceTags(id, input.tags());

        return id;
    }

    public int update(long id, PostRequest input) {
        int updated = jdbc.update(
                "UPDATE posts SET title = ?, body = ? WHERE id = ?",
                input.title(),
                input.text(),
                id
        );

        if (updated > 0) {
            replaceTags(id, input.tags());
        }

        return updated;
    }

    private void replaceTags(long id, List<String> tags) {
        jdbc.update(
                "DELETE FROM post_tags WHERE post_id = ?",
                id
        );

        if (tags != null) {
            tags.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .forEach(tag -> jdbc.update(
                            "INSERT INTO post_tags(post_id, tag) " +
                                    "VALUES (?, ?)",
                            id,
                            tag
                    ));
        }
    }

    public int delete(long id) {
        return jdbc.update(
                "DELETE FROM posts WHERE id = ?",
                id
        );
    }

    public int incrementLikes(long id) {
        int updated = jdbc.update(
                "UPDATE posts " +
                        "SET likes_count = likes_count + 1 " +
                        "WHERE id = ?",
                id
        );

        if (updated == 0) {
            return -1;
        }

        return Objects.requireNonNull(
                jdbc.queryForObject(
                        "SELECT likes_count FROM posts WHERE id = ?",
                        Integer.class,
                        id
                )
        );
    }

    public void saveImage(
            long id,
            byte[] bytes,
            String type) {

        jdbc.update(
                "UPDATE posts SET image = ?, image_type = ? " +
                        "WHERE id = ?",
                bytes,
                type,
                id
        );
    }

    public byte[] image(long id) {
        List<byte[]> bytes = jdbc.query(
                "SELECT image FROM posts WHERE id = ?",
                (rs, row) -> rs.getBytes(1),
                id
        );

        return bytes.isEmpty() ? null : bytes.getFirst();
    }

    public String imageType(long id) {
        List<String> types = jdbc.query(
                "SELECT image_type FROM posts WHERE id = ?",
                (rs, row) -> rs.getString(1),
                id
        );

        return types.isEmpty() ? null : types.getFirst();
    }
}
