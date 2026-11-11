package io.github.kiins3.vnkit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VnTextTest {

  @Test
  void removesAccents() {
    assertThat(VnText.removeAccents("Đà Nẵng")).isEqualTo("Da Nang");
    assertThat(VnText.removeAccents("Thừa Thiên Huế")).isEqualTo("Thua Thien Hue");
    assertThat(VnText.removeAccents("Tiếng Việt có dấu: ắ ặ ẫ ợ ữ ỹ")).isEqualTo("Tieng Viet co dau: a a a o u y");
    assertThat(VnText.removeAccents("plain ASCII 123")).isEqualTo("plain ASCII 123");
  }

  @Test
  void wordsIgnorePunctuationAndCase() {
    assertThat(VnText.words("TP. Hồ Chí-Minh,")).hasSize(4);
    assertThat(VnText.words("TP. Hồ Chí-Minh,")).isEqualTo(VnText.words("tp hồ chí minh"));
  }

  @Test
  void oldAndNewTonePlacementGiveTheSameWord() {
    assertThat(VnText.words("Hoà")).isEqualTo(VnText.words("Hòa"));
    assertThat(VnText.words("Thuỷ")).isEqualTo(VnText.words("Thủy"));
    assertThat(VnText.words("Hoà")).isNotEqualTo(VnText.words("Hoa"));
    assertThat(VnText.words("Hà")).isNotEqualTo(VnText.words("Hạ"));
  }

  @Test
  void composedAndDecomposedInputAreTheSame() {
    String composed = "Nguyễn";
    String decomposed = java.text.Normalizer.normalize(composed, java.text.Normalizer.Form.NFD);
    assertThat(VnText.words(decomposed)).isEqualTo(VnText.words(composed));
  }

  @Test
  void compact() {
    assertThat(VnText.compact("TP. Hồ Chí Minh")).isEqualTo("tphochiminh");
    assertThat(VnText.compact("ha_noi")).isEqualTo("hanoi");
  }
}
