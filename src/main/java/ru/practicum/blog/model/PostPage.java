package ru.practicum.blog.model;

import java.util.List;

public record PostPage(List<PostDto> posts, boolean hasPrev, boolean hasNext, int lastPage) { }
