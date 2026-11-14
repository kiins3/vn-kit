package io.github.kiins3.vnkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An old address converted to the current units. Made by {@link AddressConverter}.
 *
 * <p>Most old wards went whole into one current unit, and the result is exact. About 470 were
 * split between several; then {@link #ward()} is the unit that received most of the old ward and
 * {@link #candidates()} lists all of them. Only the street address can tell which one is right,
 * so check {@link #exact()} before saving a converted address without review.
 */
public final class Conversion {

  private final String street;
  private final LegacyProvince oldProvince;
  private final LegacyDistrict oldDistrict;
  private final LegacyWard oldWard;
  private final List<Ward> candidates;

  Conversion(String street, LegacyProvince oldProvince, LegacyDistrict oldDistrict, LegacyWard oldWard, List<Ward> candidates) {
    this.street = street == null ? "" : street.trim();
    this.oldProvince = Objects.requireNonNull(oldProvince);
    this.oldDistrict = Objects.requireNonNull(oldDistrict);
    this.oldWard = oldWard;
    this.candidates = List.copyOf(candidates);
    if (this.candidates.isEmpty()) throw new IllegalArgumentException("no current ward");
  }

  /** The current ward, commune or special zone; for a divided old ward, the one that got most of it. */
  public Ward ward() {
    return candidates.get(0);
  }

  /** The current province. */
  public Province province() {
    return ward().province();
  }

  /** Every current unit the old ward went into, the main one first. One element when {@link #exact()}. */
  public List<Ward> candidates() {
    return candidates;
  }

  /** True when the old ward went whole into one current unit, so {@link #ward()} is certain. */
  public boolean exact() {
    return candidates.size() == 1;
  }

  /** What came before the ward in the input, such as the house number and street; empty if none. */
  public String street() {
    return street;
  }

  public LegacyProvince oldProvince() {
    return oldProvince;
  }

  public LegacyDistrict oldDistrict() {
    return oldDistrict;
  }

  /** The old ward; empty for the island districts that had none (they became special zones). */
  public Optional<LegacyWard> oldWard() {
    return Optional.ofNullable(oldWard);
  }

  /** The address in the current form: {@code "12 Trần Thái Tông, Phường Cầu Giấy, Thành phố Hà Nội"}. */
  public String address() {
    List<String> parts = new ArrayList<>();
    if (!street.isEmpty()) parts.add(street);
    parts.add(ward().fullName());
    parts.add(province().fullName());
    return String.join(", ", parts);
  }

  /** The address as it was: {@code "Phường Dịch Vọng, Quận Cầu Giấy, Thành phố Hà Nội"}. */
  public String oldAddress() {
    List<String> parts = new ArrayList<>();
    if (!street.isEmpty()) parts.add(street);
    if (oldWard != null) parts.add(oldWard.fullName());
    parts.add(oldDistrict.fullName());
    parts.add(oldProvince.fullName());
    return String.join(", ", parts);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Conversion c
        && street.equals(c.street)
        && oldDistrict.equals(c.oldDistrict)
        && Objects.equals(oldWard, c.oldWard)
        && candidates.equals(c.candidates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(street, oldDistrict, oldWard, candidates);
  }

  @Override
  public String toString() {
    return oldAddress() + " -> " + address() + (exact() ? "" : " (divided: " + candidates.size() + " candidates)");
  }
}
