package by.taverna.shlyapnika.gallery;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;

public final class GalleryTextFormatter {
  private static final Set<String> PLAIN_TAGS = Set.of(
      "p", "br", "strong", "em", "u", "del", "code", "pre", "ul", "ol", "li", "h3"
  );
  private static final Pattern HREF = Pattern.compile("href\\s*=\\s*\"(https?://[^\"]+)\"", Pattern.CASE_INSENSITIVE);

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

  public static String sanitizeStoryHtml(String input) {
    var source = input == null ? "" : input.trim();
    if (source.isBlank()) return null;

    var html = new StringBuilder();
    var index = 0;
    while (index < source.length()) {
      var open = source.indexOf('<', index);
      if (open < 0) {
        html.append(source.substring(index));
        break;
      }
      html.append(source, index, open);
      var close = source.indexOf('>', open + 1);
      if (close < 0) break;
      html.append(normalizeTag(source.substring(open + 1, close)));
      index = close + 1;
    }

    var result = html.toString().trim();
    return result.isBlank() ? null : result;
  }

  private static String normalizeTag(String rawTag) {
    var tag = rawTag == null ? "" : rawTag.trim();
    if (tag.isBlank() || tag.startsWith("!") || tag.startsWith("?")) return "";

    var closing = tag.startsWith("/");
    var body = closing ? tag.substring(1).trim() : tag;
    var name = body.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    if (name.endsWith("/")) name = name.substring(0, name.length() - 1);

    if ("span".equals(name)) {
      if (closing) return "</span>";
      if (body.contains("tg-underline")) return "<span class=\"tg-underline\">";
      if (body.contains("tg-spoiler")) return "<span class=\"tg-spoiler\">";
      return "";
    }

    if ("blockquote".equals(name)) {
      if (closing) return "</blockquote>";
      return body.contains("tg-expandable-quote") ? "<blockquote class=\"tg-expandable-quote\">" : "<blockquote>";
    }

    if ("a".equals(name)) {
      if (closing) return "</a>";
      var matcher = HREF.matcher(body);
      if (!matcher.find()) return "";
      return "<a href=\"" + HtmlUtils.htmlEscape(matcher.group(1)) + "\" target=\"_blank\" rel=\"noreferrer\">";
    }

    if (!PLAIN_TAGS.contains(name)) return "";
    if ("br".equals(name)) return "<br>";
    return closing ? "</" + name + ">" : "<" + name + ">";
  }
}
