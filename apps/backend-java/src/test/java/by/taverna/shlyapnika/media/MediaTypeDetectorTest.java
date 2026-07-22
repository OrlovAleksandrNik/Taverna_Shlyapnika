package by.taverna.shlyapnika.media;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MediaTypeDetectorTest {
  @Test
  void detectsJpegSignature() {
    assertThat(MediaTypeDetector.detect(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0}))
        .hasValue(new MediaTypeDetector.DetectedMediaType("image/jpeg", "jpg"));
  }

  @Test
  void detectsPngSignature() {
    assertThat(MediaTypeDetector.detect(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0, 0, 0, 0, 0, 0, 0, 0}))
        .hasValue(new MediaTypeDetector.DetectedMediaType("image/png", "png"));
  }

  @Test
  void rejectsUnknownSignature() {
    assertThat(MediaTypeDetector.detect(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})).isEmpty();
  }
}
