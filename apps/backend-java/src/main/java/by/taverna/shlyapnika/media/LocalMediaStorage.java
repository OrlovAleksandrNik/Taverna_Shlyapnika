package by.taverna.shlyapnika.media;

import by.taverna.shlyapnika.common.Ids;
import by.taverna.shlyapnika.config.TavernaProperties;
import java.io.ByteArrayInputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "taverna.media", name = "storage", havingValue = "local", matchIfMissing = true)
public class LocalMediaStorage implements MediaStorage {
  private static final int MAX_FILE_SIZE = 50 * 1024 * 1024;
  private final TavernaProperties properties;

  public LocalMediaStorage(TavernaProperties properties) {
    this.properties = properties;
  }

  @Override
  public StoredMedia store(MediaUpload upload) {
    if (upload.bytes() == null || upload.bytes().length == 0) throw new IllegalArgumentException("Файл пустой.");
    if (upload.bytes().length > MAX_FILE_SIZE) throw new IllegalArgumentException("Файл слишком большой. Максимум 50 МБ.");
    var detected = MediaTypeDetector.detect(upload.bytes()).orElseThrow(() -> new IllegalArgumentException("Поддерживаются только JPEG, PNG, WEBP и GIF."));
    try {
      var namespace = safeNamespace(upload.namespace());
      var filename = Ids.newId("media") + "." + detected.extension();
      var storageKey = namespace + "/" + filename;
      var target = root().resolve(storageKey).normalize();
      if (!target.startsWith(root())) throw new IllegalArgumentException("Некорректный путь файла.");
      Files.createDirectories(target.getParent());
      Files.write(target, upload.bytes());
      var size = imageSize(upload.bytes(), detected.mimeType());
      var url = publicUrl(storageKey);
      return new StoredMedia(storageKey, url, url, url, detected.mimeType(), size.width(), size.height(), upload.bytes().length, upload.altText());
    } catch (AccessDeniedException error) {
      throw new IllegalStateException("Нет прав на папку загрузок. Проверьте FILE_STORAGE_DIR или права Railway volume.", error);
    } catch (Exception error) {
      if (error instanceof IllegalArgumentException illegalArgumentException) throw illegalArgumentException;
      throw new IllegalStateException("Не удалось сохранить файл.", error);
    }
  }

  @Override
  public void delete(String storageKey) {
    try {
      var path = root().resolve(storageKey).normalize();
      if (path.startsWith(root())) Files.deleteIfExists(path);
    } catch (Exception ignored) {
      // Delete is best-effort; audit is handled by the caller.
    }
  }

  @Override
  public Resource load(String storageKey) {
    return new PathResource(root().resolve(storageKey).normalize());
  }

  @Override
  public boolean exists(String storageKey) {
    var path = root().resolve(storageKey).normalize();
    return path.startsWith(root()) && Files.exists(path);
  }

  private Path root() {
    return Path.of(properties.fileStorageDir()).toAbsolutePath().normalize();
  }

  private String publicUrl(String storageKey) {
    return properties.publicUploadsUrl().replaceAll("/+$", "") + "/" + storageKey.replace("\\", "/");
  }

  private String safeNamespace(String namespace) {
    var value = namespace == null || namespace.isBlank() ? "media" : namespace;
    return value.replaceAll("[^a-zA-Z0-9/_-]", "").replaceAll("^/+", "").replaceAll("/+$", "");
  }

  private ImageSize imageSize(byte[] bytes, String mimeType) throws Exception {
    if ("image/webp".equals(mimeType)) return new ImageSize(null, null);
    var image = ImageIO.read(new ByteArrayInputStream(bytes));
    return image == null ? new ImageSize(null, null) : new ImageSize(image.getWidth(), image.getHeight());
  }

  private record ImageSize(Integer width, Integer height) {
  }
}
