package ru.practicum.blog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.blog.controller.PostController;
import ru.practicum.blog.model.*;
import ru.practicum.blog.service.BlogService;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostControllerMvcTest {
    private MockMvc mvc;
    private BlogService service;
    @BeforeEach void setUp() {
        service = mock(BlogService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PostController(service)).build();
    }
    @Test void listsPosts() throws Exception {
        when(service.list("", 1, 5)).thenReturn(new PostPage(List.of(), false, false, 1));
        mvc.perform(get("/api/posts").param("search", "").param("pageNumber", "1").param("pageSize", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.lastPage").value(1));
    }
    @Test void createsPost() throws Exception {
        when(service.create(any())).thenReturn(new PostDto(1, "Title", "Text", List.of(), 0, 0));
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Title\",\"text\":\"Text\",\"tags\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }
}
