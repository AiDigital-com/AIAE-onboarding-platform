import { MyLessonsPanel } from "./MyLessonsPanel";
import "./my-lessons.css";

export function MyLessonsPage() {
    return (
        <div className="my-lessons">
            <div className="my-lessons__inner">
                <header className="my-lessons__hero">
                    <p className="my-lessons__eyebrow">Personal learning</p>
                    <h1 className="my-lessons__title">My Lessons</h1>
                    <p className="my-lessons__lede">
                        Lessons you add from the library will appear here. Open any card to continue reading.
                    </p>
                </header>
                <MyLessonsPanel />
            </div>
        </div>
    );
}
