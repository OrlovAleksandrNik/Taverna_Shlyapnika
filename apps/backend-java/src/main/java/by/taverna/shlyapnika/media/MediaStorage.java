package by.taverna.shlyapnika.media;

import org.springframework.core.io.Resource;

public interface MediaStorage {
  StoredMedia store(MediaUpload upload);

  void delete(String storageKey);

  Resource load(String storageKey);

  boolean exists(String storageKey);
}
