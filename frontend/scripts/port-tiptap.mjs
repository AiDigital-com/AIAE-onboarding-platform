#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import * as sass from "sass";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../..");
const SRC = path.join(ROOT, "src/components/tiptap");
const DEST = path.join(ROOT, "frontend/src/shared/editor");

const HOOK_FILES = new Set([
  "use-composed-ref",
  "use-cursor-visibility",
  "use-element-rect",
  "use-is-breakpoint",
  "use-menu-navigation",
  "use-throttled-callback",
  "use-tiptap-editor",
  "use-unmount",
  "use-window-size",
]);

function transformImports(content) {
  let result = content;

  result = result.replace(/['"]use client['"];?\s*/g, "");
  result = result.replace(/["']use client["']\s*/g, "");

  const replacements = [
    [/@\/components\/tiptap\//g, "@/shared/editor/"],
    [/@\/components\/tiptap-([a-z-]+)\//g, "@/shared/editor/tiptap-$1/"],
    [/@\/lib\/tiptap-utils/g, "@/shared/editor/lib/tiptap-utils"],
    [/@\/hooks\/(use-[a-z-]+)/g, (_, hook) => {
      if (HOOK_FILES.has(hook)) {
        return `@/shared/editor/hooks/${hook}`;
      }
      return `@/shared/editor/hooks/${hook}`;
    }],
    [/from\s+["']([^"']+)\.scss["']/g, 'from "$1.css"'],
    [/import\s+["']([^"']+)\.scss["']/g, 'import "$1.css"'],
  ];

  for (const [pattern, replacement] of replacements) {
    result = result.replace(pattern, replacement);
  }

  return result;
}

function getDestPath(srcPath, ext) {
  const rel = path.relative(SRC, srcPath);
  const parsed = path.parse(rel);

  if (ext === "scss") {
    return path.join(DEST, parsed.dir, `${parsed.name}.css`);
  }

  let newName = parsed.name;
  if (ext === "jsx") {
    newName = `${parsed.name}.tsx`;
  } else if (ext === "js") {
    newName = `${parsed.name}.ts`;
  } else {
    newName = parsed.base;
  }

  return path.join(DEST, parsed.dir, newName);
}

function compileScss(srcPath, destPath) {
  const result = sass.compile(srcPath, { style: "expanded" });
  fs.mkdirSync(path.dirname(destPath), { recursive: true });
  fs.writeFileSync(destPath, result.css);
}

function portFile(srcPath) {
  const ext = path.extname(srcPath).slice(1);

  if (ext === "scss") {
    compileScss(srcPath, getDestPath(srcPath, ext));
    return;
  }

  if (!["jsx", "js", "json"].includes(ext)) {
    if (ext === "d.ts") return;
    const destPath = path.join(DEST, path.relative(SRC, srcPath));
    fs.mkdirSync(path.dirname(destPath), { recursive: true });
    fs.copyFileSync(srcPath, destPath);
    return;
  }

  const content = transformImports(fs.readFileSync(srcPath, "utf8"));
  const destPath = getDestPath(srcPath, ext);
  fs.mkdirSync(path.dirname(destPath), { recursive: true });
  fs.writeFileSync(destPath, content);
}

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full);
    } else {
      portFile(full);
    }
  }
}

if (fs.existsSync(DEST)) {
  fs.rmSync(DEST, { recursive: true, force: true });
}

walk(SRC);

// Rename partial scss that were compiled from _variables.scss etc.
const stylesDir = path.join(DEST, "styles");
if (fs.existsSync(stylesDir)) {
  for (const file of fs.readdirSync(stylesDir)) {
    if (file.startsWith("_") && file.endsWith(".css")) {
      const withoutUnderscore = file.slice(1);
      fs.renameSync(
        path.join(stylesDir, file),
        path.join(stylesDir, withoutUnderscore),
      );
    }
  }
}

console.log(`Ported TipTap editor to ${DEST}`);
