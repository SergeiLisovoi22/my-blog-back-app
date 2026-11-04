package ru.practicum.blog.model;

public record CommentDto(long id, String text, long postId) { }
