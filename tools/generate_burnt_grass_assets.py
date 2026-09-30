#!/usr/bin/env python3
import json
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path


MOD_ROOT = Path(__file__).resolve().parent.parent
WORKSPACE_ROOT = MOD_ROOT.parent.parent.parent
RESOURCE_PATH = MOD_ROOT / "src/main/resources/data/better_compat_fixes/burnt_grass_replacements.json"
ASSETS_ROOT = MOD_ROOT / "src/main/resources/assets/better_compat_fixes"
DATA_ROOT = MOD_ROOT / "src/main/resources/data/better_compat_fixes"
MOD_CACHE = WORKSPACE_ROOT / "generated/cache/packwiz-downloads/mods"


def parse_id(value: str, default_namespace: str | None = None) -> tuple[str, str]:
    if ":" in value:
        namespace, path = value.split(":", 1)
        return namespace, path
    if default_namespace is None:
        raise ValueError(f"Missing namespace for {value}")
    return default_namespace, value


def load_entries():
    data = json.loads(RESOURCE_PATH.read_text())
    return data["values"]


def discover_mod_jars() -> dict[str, Path]:
    mapping = {}
    for jar_path in sorted(MOD_CACHE.glob("*.jar")):
        with zipfile.ZipFile(jar_path) as jar:
            for name in jar.namelist():
                if not name.startswith("assets/"):
                    continue
                parts = name.split("/")
                if len(parts) < 3:
                    continue
                mapping.setdefault(parts[1], jar_path)
    return mapping


JARS = discover_mod_jars()


def load_json_from_jar(jar_path: Path, asset_path: str):
    with zipfile.ZipFile(jar_path) as jar:
        with jar.open(asset_path) as handle:
            return json.loads(handle.read().decode("utf-8"))


def first_model_reference(blockstate: dict) -> str:
    if "variants" in blockstate:
        for value in blockstate["variants"].values():
            if isinstance(value, list):
                return value[0]["model"]
            return value["model"]
    if "multipart" in blockstate:
        for part in blockstate["multipart"]:
            apply = part["apply"]
            if isinstance(apply, list):
                return apply[0]["model"]
            return apply["model"]
    raise ValueError(f"Unsupported blockstate format: {blockstate}")


def read_asset_text(namespace: str, asset_type: str, relative_path: str) -> dict:
    jar_path = JARS[namespace]
    return load_json_from_jar(jar_path, f"assets/{namespace}/{asset_type}/{relative_path}.json")


def resolve_model_textures(model_ref: str, current_namespace: str) -> dict:
    namespace, path = parse_id(model_ref, current_namespace)
    model = read_asset_text(namespace, "models", path)
    textures = {}
    parent = model.get("parent")
    if parent:
        textures.update(resolve_model_textures(parent, namespace))
    textures.update(model.get("textures", {}))
    resolved = {}
    for key, value in textures.items():
        resolved[key] = resolve_texture_reference(value, textures, namespace)
    return resolved


def resolve_texture_reference(value: str, textures: dict, namespace: str) -> str:
    while value.startswith("#"):
        value = textures[value[1:]]
    resolved_namespace, resolved_path = parse_id(value, namespace)
    return f"{resolved_namespace}:{resolved_path}"


def resolve_source_textures(source_id: str) -> tuple[str | None, str | None]:
    namespace, path = parse_id(source_id)
    try:
        blockstate = read_asset_text(namespace, "blockstates", path)
        model_ref = first_model_reference(blockstate)
        textures = resolve_model_textures(model_ref, namespace)
    except Exception:
        textures = {}
    side = textures.get("side") or textures.get("north") or textures.get("all")
    bottom = textures.get("bottom") or textures.get("down") or textures.get("all") or side
    return side, bottom


def extract_texture(
    texture_ref: str | None,
    fallback_ref: str,
    destination: Path,
    burntify: bool,
    crest_overlay_path: Path | None = None,
):
    texture_ref = texture_ref or fallback_ref
    namespace, path = parse_id(texture_ref)
    if namespace == "minecraft":
        namespace, path = parse_id(fallback_ref)
    jar_path = JARS[namespace]
    asset_path = f"assets/{namespace}/textures/{path}.png"
    with zipfile.ZipFile(jar_path) as jar:
        try:
            data = jar.read(asset_path)
        except KeyError:
            fallback_ns, fallback_path = parse_id(fallback_ref)
            with zipfile.ZipFile(JARS[fallback_ns]) as fallback_jar:
                data = fallback_jar.read(f"assets/{fallback_ns}/textures/{fallback_path}.png")
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    if burntify:
        command = ["java", "-cp", str(MOD_ROOT / "tools"), "BurntifyPng", str(destination), str(destination)]
        if crest_overlay_path is not None:
            command.append(str(crest_overlay_path))
        subprocess.run(
            command,
            check=True,
            cwd=MOD_ROOT,
        )


def write_json(path: Path, payload: dict):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n")


def clean_generated_roots():
    for root in [
        ASSETS_ROOT / "blockstates",
        ASSETS_ROOT / "models/block",
        ASSETS_ROOT / "models/item",
        ASSETS_ROOT / "textures/block",
        DATA_ROOT / "loot_tables/blocks",
    ]:
        shutil.rmtree(root, ignore_errors=True)


def main():
    subprocess.run(["javac", str(MOD_ROOT / "tools/BurntifyPng.java")], check=True, cwd=MOD_ROOT)
    clean_generated_roots()
    crest_overlay_path = MOD_ROOT / "build/tmp/burnt_grass_crest_overlay.png"
    extract_texture("burnt:block/burnt_grass", "burnt:block/burnt_grass", crest_overlay_path, burntify=False)
    entries = load_entries()
    for entry in entries:
        if entry.get("target", "").startswith("burnt:"):
            continue
        source_id = entry["source"]
        namespace, path = parse_id(source_id)
        target_path = f"burnt_{path}"
        side_ref, bottom_ref = resolve_source_textures(source_id)

        blockstate_path = ASSETS_ROOT / "blockstates" / f"{target_path}.json"
        block_model_path = ASSETS_ROOT / "models/block" / f"{target_path}.json"
        item_model_path = ASSETS_ROOT / "models/item" / f"{target_path}.json"
        loot_path = DATA_ROOT / "loot_tables/blocks" / f"{target_path}.json"
        side_texture_path = ASSETS_ROOT / "textures/block" / f"{target_path}_side.png"
        bottom_texture_path = ASSETS_ROOT / "textures/block" / f"{target_path}_bottom.png"

        write_json(blockstate_path, {"variants": {"": {"model": f"better_compat_fixes:block/{target_path}"}}})
        write_json(
            block_model_path,
            {
                "parent": "block/cube",
                "textures": {
                    "down": f"better_compat_fixes:block/{target_path}_bottom",
                    "up": "burnt:block/burnt_grass",
                    "north": f"better_compat_fixes:block/{target_path}_side",
                    "east": f"better_compat_fixes:block/{target_path}_side",
                    "south": f"better_compat_fixes:block/{target_path}_side",
                    "west": f"better_compat_fixes:block/{target_path}_side",
                    "particle": f"better_compat_fixes:block/{target_path}_bottom",
                },
                "render_type": "solid",
            },
        )
        write_json(item_model_path, {"parent": f"better_compat_fixes:block/{target_path}"})
        write_json(
            loot_path,
            {
                "type": "minecraft:block",
                "pools": [
                    {
                        "rolls": 1,
                        "entries": [{"type": "minecraft:item", "name": f"better_compat_fixes:{target_path}"}],
                        "conditions": [{"condition": "minecraft:survives_explosion"}],
                    }
                ],
            },
        )

        extract_texture(
            side_ref,
            "burnt:block/burnt_grass_side",
            side_texture_path,
            burntify=True,
            crest_overlay_path=crest_overlay_path,
        )
        extract_texture(bottom_ref, "burnt:block/burnt_dirt", bottom_texture_path, burntify=True)


if __name__ == "__main__":
    with tempfile.TemporaryDirectory():
        main()
