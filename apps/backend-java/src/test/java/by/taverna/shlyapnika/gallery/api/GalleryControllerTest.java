package by.taverna.shlyapnika.gallery.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.taverna.shlyapnika.common.NotFoundException;
import by.taverna.shlyapnika.gallery.GalleryService;
import by.taverna.shlyapnika.gallery.api.GalleryResponses.GalleryPostDto;
import by.taverna.shlyapnika.gallery.api.GalleryResponses.MasterDto;
import by.taverna.shlyapnika.gallery.api.GalleryResponses.MediaDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GalleryController.class)
@AutoConfigureMockMvc(addFilters = false)
class GalleryControllerTest {
  @Autowired
  private MockMvc mvc;

  @MockBean
  private GalleryService service;

  @Test
  void returnsPublicGalleryPosts() throws Exception {
    when(service.listPublicPosts(eq(12), eq(0))).thenReturn(List.of(post()));

    mvc.perform(get("/api/gallery")
            .param("limit", "12")
            .param("offset", "0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts[0].publicId").value("night-at-table"))
        .andExpect(jsonPath("$.posts[0].title").value("Вечер за старым столом"))
        .andExpect(jsonPath("$.posts[0].media[0].fileUrl").value("/uploads/gallery/table.webp"));
  }

  @Test
  void returnsSinglePublicGalleryPost() throws Exception {
    when(service.getPublicPost("night-at-table")).thenReturn(post());

    mvc.perform(get("/api/gallery/night-at-table"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.post.publicId").value("night-at-table"))
        .andExpect(jsonPath("$.post.master.name").value("Шляпник"))
        .andExpect(jsonPath("$.post.storyHtml").value("<p>История вечера.</p>"));
  }

  @Test
  void returnsNotFoundForHiddenOrMissingPost() throws Exception {
    when(service.getPublicPost("hidden-post"))
        .thenThrow(new NotFoundException("Публикация не найдена."));

    mvc.perform(get("/api/gallery/hidden-post"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  private static GalleryPostDto post() {
    return new GalleryPostDto(
        "gal_1",
        "night-at-table",
        "story",
        "Вечер за старым столом",
        "Короткое описание публикации.",
        "<p>История вечера.</p>",
        "tavern",
        Instant.parse("2026-07-18T00:00:00Z"),
        new MasterDto("mst_1", "Шляпник"),
        List.of(new MediaDto(
            "med_1",
            "/uploads/gallery/table.webp",
            "/uploads/gallery/table-thumb.webp",
            "/uploads/gallery/table-medium.webp",
            1200,
            900,
            "image/webp",
            "Игровой стол в Таверне Шляпника"
        )),
        Instant.parse("2026-07-19T12:00:00Z"),
        Instant.parse("2026-07-19T12:30:00Z")
    );
  }
}
