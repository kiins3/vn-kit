"""Builds the province and ward tables bundled in vn-kit-core.

Source: thanglequoc/vietnamese-provinces-database (MIT), JSON export.

    python scripts/build_data.py                 # download the pinned release
    python scripts/build_data.py path/to/full_json_generated_data_vn_units.json

Writes vn-kit-core/src/main/resources/io/github/kiins3/vnkit/{provinces,wards}.tsv
"""

import json
import sys
import urllib.request
from pathlib import Path

SOURCE_COMMIT = "8b78ba5118715e1fa81769286724db79346abf52"
BASE = f"https://raw.githubusercontent.com/thanglequoc/vietnamese-provinces-database/{SOURCE_COMMIT}/json"
OUT = Path(__file__).resolve().parent.parent / "vn-kit-core/src/main/resources/io/github/kiins3/vnkit"

PROVINCE_TYPES = {1: "CITY", 2: "PROVINCE"}
WARD_TYPES = {3: "WARD", 4: "COMMUNE", 5: "SPECIAL_ZONE"}


def fetch(name: str) -> str:
    with urllib.request.urlopen(f"{BASE}/{name}") as r:
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
    if len(sys.argv) > 1:
        units = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
        meta = {"DatasetVersion": "local", "LatestDecree": "?"}
    else:
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
    for name, cols, rows in [
        ("provinces.tsv", "code\tname\tnameEn\tfullName\tfullNameEn\tcodeName\ttype\tpostalPrefixes", provinces),
        ("wards.tsv", "code\tprovinceCode\tname\tnameEn\tfullName\tfullNameEn\tcodeName\ttype\tpostalCode", wards),
    ]:
        lines = [header, f"# {cols}\n"] + ["\t".join(clean(c) for c in row) + "\n" for row in rows]
        (OUT / name).write_text("".join(lines), encoding="utf-8", newline="\n")

    print(f"{len(provinces)} provinces, {len(wards)} wards ({meta['DatasetVersion']}, {meta['LatestDecree']})")


if __name__ == "__main__":
    main()
