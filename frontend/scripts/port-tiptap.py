#!/usr/bin/env python3
"""Port TipTap editor from Next.js src to frontend shared/editor."""

from __future__ import annotations

import os
import re
import shutil
from pathlib import Path

import sass

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "src/components/tiptap"
DEST = ROOT / "frontend/src/shared/editor"

HOOK_FILES = {
    "use-composed-ref",
    "use-cursor-visibility",
    "use-element-rect",
    "use-is-breakpoint",
    "use-menu-navigation",
    "use-throttled-callback",
    "use-tiptap-editor",
    "use-unmount",
    "use-window-size",
}


def transform_imports(content: str) -> str:
    content = re.sub(r"""['"]use client['"];?\s*""", "", content)

    content = content.replace("@/components/tiptap/", "@/shared/editor/")
    content = re.sub(
        r"@/components/tiptap-([a-z-]+)/",
        r"@/shared/editor/tiptap-\1/",
        content,
    )
    content = content.replace("@/lib/tiptap-utils", "@/shared/editor/lib/tiptap-utils")

    def replace_hook(match: re.Match[str]) -> str:
        hook = match.group(1)
        if hook in HOOK_FILES:
            return f"@/shared/editor/hooks/{hook}"
        return f"@/shared/editor/hooks/{hook}"

    content = re.sub(r"@/hooks/(use-[a-z-]+)", replace_hook, content)
    content = re.sub(r'from\s+["\']([^"\']+)\.scss["\']', r'from "\1.css"', content)
    content = re.sub(r'import\s+["\']([^"\']+)\.scss["\']', r'import "\1.css"', content)
    return content


def dest_path_for(src_path: Path) -> Path:
    rel = src_path.relative_to(SRC)
    suffix = rel.suffix.lower()

    if suffix == ".scss":
        return DEST / rel.with_suffix(".css")

    if suffix == ".jsx":
        return DEST / rel.with_suffix(".tsx")

    if suffix == ".js":
        return DEST / rel.with_suffix(".ts")

    return DEST / rel


def preprocess_scss(content: str) -> str:
    # libsass does not understand modern space-separated rgb() syntax.
    content = re.sub(
        r"rgb\(\s*(\d+)\s+(\d+)\s+(\d+)\s*/\s*([^)]+)\)",
        r"rgba(\1, \2, \3, \4)",
        content,
    )
    return content


def port_scss(src_path: Path, dest_path: Path) -> None:
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    source = preprocess_scss(src_path.read_text(encoding="utf-8"))
    compiled = sass.compile(string=source, output_style="expanded")
    dest_path.write_text(compiled, encoding="utf-8")


def port_file(src_path: Path) -> None:
    suffix = src_path.suffix.lower()

    if suffix == ".scss":
        port_scss(src_path, dest_path_for(src_path))
        return

    if suffix in {".jsx", ".js"}:
        dest = dest_path_for(src_path)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(transform_imports(src_path.read_text(encoding="utf-8")), encoding="utf-8")
        return

    if suffix == ".d.ts":
        return

    dest = dest_path_for(src_path)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src_path, dest)


def main() -> None:
    if DEST.exists():
        shutil.rmtree(DEST)

    for src_path in sorted(SRC.rglob("*")):
        if src_path.is_file():
            port_file(src_path)

    styles_dir = DEST / "styles"
    if styles_dir.exists():
        for css_file in styles_dir.glob("_*.css"):
            css_file.rename(styles_dir / css_file.name[1:])

    print(f"Ported TipTap editor to {DEST}")


if __name__ == "__main__":
    main()
