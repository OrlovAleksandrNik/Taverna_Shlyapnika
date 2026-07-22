package by.taverna.shlyapnika.media;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "taverna.media", name = "storage", havingValue = "object")
public class ObjectStorageMediaStorage implements MediaStorage {
  @Override
  public StoredMedia store(MediaUpload upload) {
    throw new UnsupportedOperationException("Object storage is configured but not implemented yet.");
  }

  @Override
  public void delete(String storageKey) {
    throw new UnsupportedOperationException("Object storage is configured but not implemented yet.");
  }

  @Override
  public Resource load(String storageKey) {
    throw new UnsupportedOperationException("Object storage is configured but not implemented yet.");
  }

  @Override
  public boolean exists(String storageKey) {
    throw new UnsupportedOperationException("Object storage is configured but not implemented yet.");
  }
}
