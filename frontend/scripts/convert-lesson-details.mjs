#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.resolve(__dirname, "../../src/components/lessons/LessonDetailsDialog.jsx");
const DEST = path.resolve(__dirname, "../src/features/library/ui/LessonDetailsDialog.tsx");

let content = fs.readFileSync(SRC, "utf8");

content = content.replace(/^'use client';\s*/m, "");
content = content.replace(/export default function LessonDetailsDialog/, "export function LessonDetailsDialog");

const importBlock = `import { useEffect, useMemo, useRef, useState } from "react";
import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";
import { SimpleEditor } from "@/shared/editor/tiptap-templates/simple/simple-editor";
import { markdownToHtml } from "@/shared/lib/lessonContent";
import { AI_DIGITAL_COLORS, hexToRgba } from "@/shared/lib/brandColors";
import { normalizeLessonTagInput, suggestedLessonTags } from "@/shared/lib/lessonTags";
import { LessonReader } from "./LessonReader";
import { LessonAttachments, getSourceAttachments } from "./LessonAttachments";
import type { LibraryLesson } from "../api/types";
import "./lesson-details-dialog.css";
`;

content = content.replace(/^import[\s\S]*?from '\.\/LessonReader';[\s\S]*?from '\.\.\/\.\.\/lib\/lessonTags';\n/m, "");

content = `/* eslint-disable @typescript-eslint/no-explicit-any */\n${importBlock}\n${content}`;

content = content
    .replace(/lesson\?:/g, "lesson:")
    .replace(/onLessonDeleted,/g, "onLessonDeleted,")
    .replace(/fetch\(`\/api\//g, "fetch(`/api/v1/")
    .replace(/fetch\('\/api\//g, "fetch('/api/v1/")
    .replace(/`\/api\/files\/object/g, "`/api/v1/files/object");

fs.mkdirSync(path.dirname(DEST), { recursive: true });
fs.writeFileSync(DEST, content);
console.log(`Wrote ${DEST}`);
