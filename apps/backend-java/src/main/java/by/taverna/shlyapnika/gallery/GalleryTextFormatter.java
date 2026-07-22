package by.taverna.shlyapnika.gallery;

import org.springframework.web.util.HtmlUtils;

public final class GalleryTextFormatter {
  private GalleryTextFormatter() {
  }

  public static String formatStory(String input) {
    var text = input == null ? "" : input.trim();
    if (text.isBlank()) return null;

    var blocks = text.split("\\R{2,}");
    var html = new StringBuilder();
    for (var block : blocks) {
      var trimmed = block.trim();
      if (trimmed.isBlank()) continue;
      var escaped = HtmlUtils.htmlEscape(trimmed);
      if (trimmed.matches("^#{2,3}\\s+.*")) {
        html.append("<h3>").append(escaped.replaceFirst("^#{2,3}\\s+", "")).append("</h3>");
      } else if (trimmed.matches("^>\\s+.*")) {
        html.append("<blockquote>").append(escaped.replaceFirst("^&gt;\\s+", "")).append("</blockquote>");
      } else if (trimmed.matches("(?s)^[-*]\\s+.*")) {
        html.append("<ul>");
        for (var line : trimmed.split("\\R")) {
          var item = line.replaceFirst("^[-*]\\s+", "").trim();
          if (!item.isBlank()) html.append("<li>").append(HtmlUtils.htmlEscape(item)).append("</li>");
        }
        html.append("</ul>");
      } else {
        html.append("<p>")
            .append(escaped
                .replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>")
                .replaceAll("\\*([^*]+)\\*", "<em>$1</em>")
                .replaceAll("\\R", "<br>"))
            .append("</p>");
      }
    }
    return html.toString();
  }
}
