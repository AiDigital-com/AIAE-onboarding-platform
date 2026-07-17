export function LessonsLoadingState({ showAction = true }: { showAction?: boolean }) {
    return (
        <div className="library-skeleton-grid">
            {Array.from({ length: 6 }, (_, index) => (
                <article key={index} className="library-skeleton-card">
                    <div className="library-skeleton-card__media" />
                    <div className="library-skeleton-card__body">
                        <div className="library-skeleton-line" style={{ width: "88%" }} />
                        <div className="library-skeleton-line" style={{ width: "72%" }} />
                        <div className="library-skeleton-line" />
                        {showAction && <div className="library-skeleton-line" style={{ width: "45%", marginTop: 16 }} />}
                    </div>
                </article>
            ))}
        </div>
    );
}
