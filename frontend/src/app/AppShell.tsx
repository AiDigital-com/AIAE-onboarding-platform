import { ReactNode } from "react";
import { useLocation } from "react-router-dom";
import { Sidebar } from "../shared/ui/Sidebar";
import "../shared/ui/app-shell.css";

interface Props {
    children: ReactNode;
}

/** Returns true for /lessons/:id (reader) but NOT /lessons/:id/activities/... */
function isLessonReadingRoute(pathname: string): boolean {
    return /^\/lessons\/[^/]+\/?$/.test(pathname);
}

/** App shell — sidebar on the left except in lesson-reader mode. */
export function AppShell({ children }: Props) {
    const { pathname } = useLocation();
    const readingMode = isLessonReadingRoute(pathname);

    return (
        <div className="app-shell">
            {!readingMode && <Sidebar />}
            <main
                className={[
                    "app-shell__main",
                    readingMode ? "app-shell__main--reading" : "",
                ]
                    .filter(Boolean)
                    .join(" ")}
            >
                {children}
            </main>
        </div>
    );
}
