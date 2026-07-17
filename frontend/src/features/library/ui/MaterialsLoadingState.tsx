export function MaterialsLoadingState() {
    return (
        <div className="library-skeleton-grid">
            {Array.from({ length: 6 }, (_, index) => (
                <article key={index} className="library-skeleton-card">
                    <div className="library-skeleton-card__media" />
                    <div className="library-skeleton-card__body">
                        <div className="library-skeleton-line" style={{ width: "70%" }} />
                        <div className="library-skeleton-line" />
                        <div className="library-skeleton-line" style={{ width: "85%" }} />
                    </div>
                </article>
            ))}
        </div>
    );
}
