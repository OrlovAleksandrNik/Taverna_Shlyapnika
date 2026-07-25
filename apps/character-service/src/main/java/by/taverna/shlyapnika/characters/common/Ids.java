package by.taverna.shlyapnika.characters.common;

import java.util.UUID;

public final class Ids {
  private Ids() {
  }

  public static String newId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
  }
}
