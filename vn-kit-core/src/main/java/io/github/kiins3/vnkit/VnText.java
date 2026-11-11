package io.github.kiins3.vnkit;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Text helpers for Vietnamese: removing accents and building keys for matching names. */
public final class VnText {

  private VnText() {}

  /** Removes Vietnamese accents: {@code "Đà Nẵng"} becomes {@code "Da Nang"}. */
  public static String removeAccents(String text) {
    String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
    StringBuilder out = new StringBuilder(nfd.length());
    for (int i = 0; i < nfd.length(); i++) {
      char c = nfd.charAt(i);
      if (Character.getType(c) == Character.NON_SPACING_MARK) continue;
      out.append(c == 'đ' ? 'd' : c == 'Đ' ? 'D' : c);
    }
    return Normalizer.normalize(out, Normalizer.Form.NFC);
  }

  /**
   * Splits text into lower-case words with accents kept, ignoring punctuation. Tone marks are
   * moved to the end of each word, so the old and new tone placements compare equal
   * ({@code "Hoà"} and {@code "Hòa"} give the same word).
   */
  static List<String> words(String text) {
    String nfd = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
    List<String> words = new ArrayList<>();
    StringBuilder base = new StringBuilder();
    StringBuilder tones = new StringBuilder();
    for (int i = 0; i < nfd.length(); i++) {
      char c = nfd.charAt(i);
      if (Character.getType(c) == Character.NON_SPACING_MARK) {
        if (base.length() == 0) continue;
        (isTone(c) ? tones : base).append(c);
      } else if (Character.isLetterOrDigit(c)) {
        base.append(c);
      } else {
        flush(words, base, tones);
      }
    }
    flush(words, base, tones);
    return words;
  }

  /** The word without any accents: {@code "đình"} becomes {@code "dinh"}. */
  static String plain(String word) {
    StringBuilder out = new StringBuilder(word.length());
    for (int i = 0; i < word.length(); i++) {
      char c = word.charAt(i);
      if (Character.getType(c) == Character.NON_SPACING_MARK) continue;
      out.append(c == 'đ' ? 'd' : c);
    }
    return out.toString();
  }

  /** Lower-case, accent-free and without spaces: {@code "TP. Hồ Chí Minh"} gives {@code "tphochiminh"}. */
  static String compact(String text) {
    StringBuilder out = new StringBuilder();
    for (String w : words(text)) out.append(plain(w));
    return out.toString();
  }

  private static boolean isTone(char c) {
    // grave, acute, tilde, hook above, dot below
    return c == '̀' || c == '́' || c == '̃' || c == '̉' || c == '̣';
  }

  private static void flush(List<String> words, StringBuilder base, StringBuilder tones) {
    if (base.length() > 0) words.add(base.append(tones).toString());
    base.setLength(0);
    tones.setLength(0);
  }
}
