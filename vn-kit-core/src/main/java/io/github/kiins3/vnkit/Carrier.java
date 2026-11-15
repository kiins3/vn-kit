package io.github.kiins3.vnkit;

import java.util.List;

/**
 * The mobile network a prefix was assigned to. Since 2018 subscribers can switch networks and
 * keep their number, so this is the network that issued the number, not necessarily the one it
 * is on today.
 */
public enum Carrier {
  VIETTEL("Viettel", "032", "033", "034", "035", "036", "037", "038", "039", "086", "095", "096", "097", "098"),
  VINAPHONE("VinaPhone", "081", "082", "083", "084", "085", "088", "091", "094"),
  MOBIFONE("MobiFone", "070", "076", "077", "078", "079", "089", "090", "093"),
  VIETNAMOBILE("Vietnamobile", "052", "056", "058", "092"),
  GMOBILE("Gmobile", "059", "099"),
  ITEL("iTel", "087"),
  WINTEL("Wintel", "055"),
  POTTEL("Pottel", "075");

  private final String label;
  private final List<String> prefixes;

  Carrier(String label, String... prefixes) {
    this.label = label;
    this.prefixes = List.of(prefixes);
  }

  /** Brand name, e.g. {@code "VinaPhone"}. */
  public String label() {
    return label;
  }

  /** Its three-digit prefixes including the leading zero, e.g. {@code "091"}. */
  public List<String> prefixes() {
    return prefixes;
  }
}
