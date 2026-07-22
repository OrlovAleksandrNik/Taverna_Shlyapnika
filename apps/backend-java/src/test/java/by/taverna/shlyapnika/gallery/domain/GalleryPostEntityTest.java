package by.taverna.shlyapnika.gallery.domain;

import static org.assertj.core.api.Assertions.assertThat;

import by.taverna.shlyapnika.master.domain.MasterEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class GalleryPostEntityTest {
  @Test
  void publishedPostGetsPublishedAtAndHiddenPostBecomesInvisible() {
    var master = MasterEntity.create(123L, "hatter", "Шляпник", "https://t.me/hatter");
    var post = GalleryPostEntity.create(
        "gal_test",
        "story",
        "История",
        "Короткое описание",
        "Текст",
        "<p>Текст</p>",
        "tavern",
        null,
        master,
        "published",
        List.of()
    );

    assertThat(post.getStatus()).isEqualTo("published");
    assertThat(post.getPublishedAt()).isNotNull();

    post.setStatus("hidden");

    assertThat(post.getStatus()).isEqualTo("hidden");
    assertThat(post.getIsVisible()).isFalse();
  }

  @Test
  void attachesMediaToPost() {
    var master = MasterEntity.create(123L, "hatter", "Шляпник", "https://t.me/hatter");
    var media = GalleryMediaEntity.create("/a.jpg", "/a-thumb.jpg", "/a-medium.jpg", 100, 80, "image/jpeg", "Афиша", 0);

    var post = GalleryPostEntity.create("gal_test", "photo", "Фото", null, null, null, "games", null, master, "draft", List.of(media));

    assertThat(post.getMedia()).hasSize(1);
    assertThat(post.getMedia().getFirst().getFileUrl()).isEqualTo("/a.jpg");
  }
}
