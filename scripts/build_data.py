"""Builds the tables bundled in vn-kit-core.

Current units (34 provinces, 3,321 wards):
    thanglequoc/vietnamese-provinces-database (MIT), JSON export.
Units before 1 July 2025 (63 provinces, districts, wards) and how they map to the current ones:
    tranngocminhhieu/vietnamadminunits (MIT), data/processed.

    python scripts/build_data.py

Writes vn-kit-core/src/main/resources/io/github/kiins3/vnkit/*.tsv
"""

import csv
import io
import json
import urllib.request
from collections import defaultdict
from pathlib import Path

SOURCE_COMMIT = "8b78ba5118715e1fa81769286724db79346abf52"
BASE = f"https://raw.githubusercontent.com/thanglequoc/vietnamese-provinces-database/{SOURCE_COMMIT}/json"
LEGACY_COMMIT = "7fac8c45805aad9916b17237c54baf4502303b93"
LEGACY_BASE = f"https://raw.githubusercontent.com/tranngocminhhieu/vietnamadminunits/{LEGACY_COMMIT}/data/processed"
OUT = Path(__file__).resolve().parent.parent / "vn-kit-core/src/main/resources/io/github/kiins3/vnkit"

PROVINCE_TYPES = {1: "CITY", 2: "PROVINCE"}
WARD_TYPES = {3: "WARD", 4: "COMMUNE", 5: "SPECIAL_ZONE"}


def fetch(name: str, base: str = BASE) -> str:
    with urllib.request.urlopen(f"{base}/{name}") as r:
        return r.read().decode("utf-8")


def clean(value: str) -> str:
    value = " ".join(str(value).split())
    if "\t" in value or "\n" in value:
        raise ValueError(f"tab or newline in {value!r}")
    return value


def full_name(unit: dict) -> str:
    # The source has a few full names that start in lower case ("xã Bắc Sơn").
    name = clean(unit["FullName"])
    return name[:1].upper() + name[1:]


def main() -> None:
    units = json.loads(fetch("full_json_generated_data_vn_units.json"))
    meta = json.loads(fetch("vn_provinces_metadata.json"))

    header = (
        f"# source=thanglequoc/vietnamese-provinces-database@{SOURCE_COMMIT[:7]} "
        f"dataset={meta['DatasetVersion']} decree={meta['LatestDecree']}\n"
    )

    provinces, wards = [], []
    for p in sorted(units, key=lambda p: p["Code"]):
        provinces.append([
            p["Code"], p["Name"], p["NameEn"], full_name(p), p["FullNameEn"], p["CodeName"],
            PROVINCE_TYPES[p["AdministrativeUnitId"]],
            ",".join(x.strip() for x in p["PostalCodePrefix"].split(",") if x.strip()),
        ])
        for w in sorted(p["Wards"], key=lambda w: w["Code"]):
            if w["ProvinceCode"] != p["Code"]:
                raise ValueError(f"ward {w['Code']} filed under the wrong province")
            wards.append([
                w["Code"], p["Code"], w["Name"], w["NameEn"], full_name(w), w["FullNameEn"], w["CodeName"],
                WARD_TYPES[w["AdministrativeUnitId"]], w["PostalCode"],
            ])

    codes = [w[0] for w in wards]
    if len(codes) != len(set(codes)):
        raise ValueError("duplicate ward codes")

    OUT.mkdir(parents=True, exist_ok=True)
    write("provinces.tsv", header, "code\tname\tnameEn\tfullName\tfullNameEn\tcodeName\ttype\tpostalPrefixes", provinces)
    write("wards.tsv", header, "code\tprovinceCode\tname\tnameEn\tfullName\tfullNameEn\tcodeName\ttype\tpostalCode", wards)
    print(f"{len(provinces)} provinces, {len(wards)} wards ({meta['DatasetVersion']}, {meta['LatestDecree']})")

    build_legacy({p[0] for p in provinces}, {w[0] for w in wards})


def write(name: str, header: str, cols: str, rows: list) -> None:
    lines = [header, f"# {cols}\n"] + ["\t".join(clean(c) for c in row) + "\n" for row in rows]
    (OUT / name).write_text("".join(lines), encoding="utf-8", newline="\n")


# ---------- Units before 1 July 2025 ----------

PROVINCE_TYPES_LEGACY = ["Thành phố", "Tỉnh"]
DISTRICT_TYPES = ["Thành phố", "Thị xã", "Quận", "Huyện"]
WARD_TYPES_LEGACY = ["Thị trấn", "Phường", "Xã"]


def split_type(full: str, types: list, fallback: str) -> tuple:
    """'Thị Trấn Năm Căn' gives ('Thị trấn', 'Năm Căn'). The source is not consistent about case."""
    full = clean(full)
    for t in types:
        if full.lower().startswith(t.lower() + " "):
            return t, full[len(t) + 1:]
    return fallback, full


def code(value: str, width: int) -> str:
    return str(int(float(value))).zfill(width)


def build_legacy(new_provinces: set, new_wards: set) -> None:
    rows = list(csv.DictReader(io.StringIO(fetch("convert_legacy_2025_with_location_and_default_ward.csv", LEGACY_BASE))))

    provinces, districts, wards = {}, {}, defaultdict(list)
    for r in rows:
        pc, dc = code(r["provinceCode"], 2), code(r["districtCode"], 3)
        new_pc, new_wc = code(r["newProvinceCode"], 2), code(r["newWardCode"], 5)
        if new_pc not in new_provinces or new_wc not in new_wards:
            raise ValueError(f"unknown current unit {new_pc}/{new_wc}")

        p_type, p_name = split_type(r["province"], PROVINCE_TYPES_LEGACY, "Tỉnh")
        province = provinces.setdefault(pc, [pc, p_name, f"{p_type} {p_name}", new_pc])
        if province[3] != new_pc:
            raise ValueError(f"old province {pc} maps to two provinces")

        d_type, d_name = split_type(r["district"], DISTRICT_TYPES, r["districtType"])
        district = districts.setdefault(dc, [dc, pc, d_name, f"{d_type} {d_name}", d_type, []])

        if not r["wardCode"]:
            # Island districts without wards became special zones.
            district[5].append(new_wc)
            continue
        wc = code(r["wardCode"], 5)
        w_type, w_name = split_type(r["ward"], WARD_TYPES_LEGACY, r["wardType"])
        # For a divided ward the default (the part with most of it) goes first.
        rank = 0 if r["isDefaultNewWard"] in ("True", "") else 1
        wards[wc].append((rank, new_wc, [wc, dc, pc, w_name, f"{w_type} {w_name}", w_type]))

    ward_rows = []
    for wc in sorted(wards):
        targets = sorted(wards[wc])
        if sum(1 for t in targets if t[0] == 0) != 1:
            raise ValueError(f"old ward {wc} needs exactly one default current ward")
        ward_rows.append(targets[0][2] + [",".join(t[1] for t in targets)])

    header = f"# source=tranngocminhhieu/vietnamadminunits@{LEGACY_COMMIT[:7]} units before 1 July 2025\n"
    write("legacy_provinces.tsv", header, "code\tname\tfullName\tnewProvinceCode",
          [provinces[k] for k in sorted(provinces)])
    write("legacy_districts.tsv", header, "code\tprovinceCode\tname\tfullName\ttype\tnewWardCodes",
          [d[:5] + [",".join(d[5])] for d in (districts[k] for k in sorted(districts))])
    write("legacy_wards.tsv", header, "code\tdistrictCode\tprovinceCode\tname\tfullName\ttype\tnewWardCodes", ward_rows)
    divided = sum(1 for r in ward_rows if "," in r[-1])
    print(f"legacy: {len(provinces)} provinces, {len(districts)} districts, {len(ward_rows)} wards ({divided} divided)")


if __name__ == "__main__":
    main()
