package by.taverna.shlyapnika.gallery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GalleryTextFormatterTest {
  @Test
  void formatsSafeParagraphsAndInlineMarks() {
    var html = GalleryTextFormatter.formatStory("Первая строка\nвторая строка\n\n**важно** и *тихо*");

    assertThat(html).contains("<p>Первая строка<br>вторая строка</p>");
    assertThat(html).contains("<strong>важно</strong>");
    assertThat(html).contains("<em>тихо</em>");
  }

  @Test
  void escapesHtmlBeforeFormatting() {
    var html = GalleryTextFormatter.formatStory("<script>alert(1)</script>");

    assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    assertThat(html).doesNotContain("<script>");
  }
}
