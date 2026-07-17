import { sanitizeLessonHtml } from "@/shared/lib/sanitizeLessonHtml";
import { useResolvedStorageHtml } from "@/shared/lib/useResolvedStorageHtml";
import "./lesson-reader.css";

interface Props {
    html: string;
}

export function LessonReader({ html }: Props) {
    const resolvedHtml = useResolvedStorageHtml(html);

    return <div className="lesson-reader" dangerouslySetInnerHTML={{ __html: sanitizeLessonHtml(resolvedHtml) }} />;
}
