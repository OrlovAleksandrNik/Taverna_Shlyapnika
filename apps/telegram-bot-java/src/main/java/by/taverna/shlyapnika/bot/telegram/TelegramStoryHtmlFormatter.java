package by.taverna.shlyapnika.bot.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.util.HtmlUtils;

public final class TelegramStoryHtmlFormatter {
  private TelegramStoryHtmlFormatter() {
  }

  public static String format(String text, JsonNode entities) {
    var source = text == null ? "" : text;
    if (source.isBlank() || entities == null || !entities.isArray() || entities.isEmpty()) return null;

    var openings = new HashMap<Integer, List<Tag>>();
    var closings = new HashMap<Integer, List<Tag>>();
    for (var entity : entities) {
      var offset = entity.path("offset").asInt(-1);
      var length = entity.path("length").asInt(0);
      if (offset < 0 || length <= 0 || offset + length > source.length()) continue;
      var tag = tagFor(source, entity, offset, length);
      if (tag == null) continue;
      openings.computeIfAbsent(offset, ignored -> new ArrayList<>()).add(tag);
      closings.computeIfAbsent(offset + length, ignored -> new ArrayList<>()).add(tag);
    }

    if (openings.isEmpty()) return null;
    openings.values().forEach(tags -> tags.sort(Comparator.comparingInt(Tag::length).reversed()));
    closings.values().forEach(tags -> tags.sort(Comparator.comparingInt(Tag::length)));

    var html = new StringBuilder();
    for (var index = 0; index <= source.length(); index++) {
      appendTags(html, closings.get(index), true);
      appendTags(html, openings.get(index), false);
      if (index < source.length()) html.append(HtmlUtils.htmlEscape(source.substring(index, index + 1)));
    }

    return toBlocks(html.toString());
  }

  private static void appendTags(StringBuilder html, List<Tag> tags, boolean close) {
    if (tags == null) return;
    if (close) {
      for (var tag : tags) html.append(tag.close());
    } else {
      for (var tag : tags) html.append(tag.open());
    }
  }

  private static Tag tagFor(String source, JsonNode entity, int offset, int length) {
    var type = entity.path("type").asText("");
    return switch (type) {
      case "bold" -> new Tag("<strong>", "</strong>", length);
      case "italic" -> new Tag("<em>", "</em>", length);
      case "underline" -> new Tag("<span class=\"tg-underline\">", "</span>", length);
      case "strikethrough" -> new Tag("<del>", "</del>", length);
      case "spoiler" -> new Tag("<span class=\"tg-spoiler\">", "</span>", length);
      case "code" -> new Tag("<code>", "</code>", length);
      case "pre" -> new Tag("<pre><code>", "</code></pre>", length);
      case "blockquote" -> new Tag("<blockquote>", "</blockquote>", length);
      case "expandable_blockquote" -> new Tag("<blockquote class=\"tg-expandable-quote\">", "</blockquote>", length);
      case "text_link" -> linkTag(entity.path("url").asText(""), length);
      case "url" -> linkTag(source.substring(offset, offset + length), length);
      default -> null;
    };
  }

  private static Tag linkTag(String url, int length) {
    var href = url == null ? "" : url.trim();
    if (!href.startsWith("http://") && !href.startsWith("https://")) return null;
    return new Tag("<a href=\"" + HtmlUtils.htmlEscape(href) + "\" target=\"_blank\" rel=\"noreferrer\">", "</a>", length);
  }

  private static String toBlocks(String inlineHtml) {
    var blocks = inlineHtml.split("\\R{2,}");
    var result = new StringBuilder();
    for (var block : blocks) {
      var trimmed = block.trim();
      if (trimmed.isBlank()) continue;
      var content = trimmed.replaceAll("\\R", "<br>");
      if (content.startsWith("<blockquote") || content.startsWith("<pre")) {
        result.append(content);
      } else {
        result.append("<p>").append(content).append("</p>");
      }
    }
    return result.length() == 0 ? null : result.toString();
  }

  private record Tag(String open, String close, int length) {
  }
}
