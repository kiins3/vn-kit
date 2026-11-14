package io.github.kiins3.vnkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loads the bundled tables once, on first use. */
final class Registry {

  private static final Pattern HEADER = Pattern.compile("dataset=(\\S+) decree=(\\S+)");

  final List<Province> provinces;
  final List<Ward> wards;
  final Map<String, Province> provincesByCode;
  final Map<String, Ward> wardsByCode;
  final Map<String, List<Ward>> wardsByProvince;
  final String datasetVersion;
  final String latestDecree;

  private Registry() {
    List<Province> ps = new ArrayList<>();
    String[] meta = read("provinces.tsv", 8, f -> ps.add(new Province(
        f[0], f[1], f[2], f[3], f[4], f[5], ProvinceType.valueOf(f[6]), Arrays.asList(f[7].split(",")))));
    List<Ward> ws = new ArrayList<>();
    read("wards.tsv", 9, f -> ws.add(new Ward(
        f[0], f[1], f[2], f[3], f[4], f[5], f[6], WardType.valueOf(f[7]), f[8])));

    provinces = List.copyOf(ps);
    wards = List.copyOf(ws);
    datasetVersion = meta[0];
    latestDecree = meta[1];

    Map<String, Province> pByCode = new LinkedHashMap<>();
    for (Province p : provinces) pByCode.put(p.code(), p);
    Map<String, Ward> wByCode = new LinkedHashMap<>();
    Map<String, List<Ward>> byProvince = new LinkedHashMap<>();
    for (Ward w : wards) {
      wByCode.put(w.code(), w);
      byProvince.computeIfAbsent(w.provinceCode(), k -> new ArrayList<>()).add(w);
    }
    byProvince.replaceAll((k, v) -> List.copyOf(v));
    provincesByCode = Map.copyOf(pByCode);
    wardsByCode = Map.copyOf(wByCode);
    wardsByProvince = Map.copyOf(byProvince);
  }

  static Registry get() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final Registry INSTANCE = new Registry();
  }

  /** Reads a tab-separated table and returns {dataset, decree} from its header comment. */
  static String[] read(String file, int columns, Consumer<String[]> row) {
    String[] meta = {"unknown", "unknown"};
    try (InputStream in = Registry.class.getResourceAsStream(file)) {
      if (in == null) throw new IllegalStateException("vn-kit data file missing: " + file);
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      for (String line; (line = reader.readLine()) != null; ) {
        if (line.startsWith("#")) {
          Matcher m = HEADER.matcher(line);
          if (m.find()) meta = new String[] {m.group(1), m.group(2)};
          continue;
        }
        if (line.isEmpty()) continue;
        String[] fields = line.split("\t", -1);
        if (fields.length != columns) {
          throw new IllegalStateException("Bad row in " + file + ": " + line);
        }
        row.accept(fields);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return meta;
  }
}
