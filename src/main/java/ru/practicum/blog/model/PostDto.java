package ru.practicum.blog.model;

import java.util.List;

public record PostDto(long id, String title, String text, List<String> tags, int likesCount, int commentsCount) { }
