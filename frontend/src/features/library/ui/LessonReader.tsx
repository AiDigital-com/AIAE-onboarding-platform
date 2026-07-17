import { sanitizeLessonHtml } from "@/shared/lib/sanitizeLessonHtml";
import { useResolvedStorageHtml } from "@/shared/lib/useResolvedStorageHtml";
import "./lesson-reader.css";

interface LessonReaderProps {
    html: string;
}

export function LessonReader({ html }: LessonReaderProps) {
    const resolvedHtml = useResolvedStorageHtml(html);

    return <div className="lesson-reader" dangerouslySetInnerHTML={{ __html: sanitizeLessonHtml(resolvedHtml) }} />;
}
