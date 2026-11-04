package ru.practicum.blog.model;

import java.util.List;

public record PostRequest(Long id, String title, String text, List<String> tags) { }
