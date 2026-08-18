-- version: optimistic-locking column. completedAt has three independent
-- writers (a manual complete/incomplete toggle, and two automatic side
-- effects of quiz/flashcard submission) that can race on the same
-- (user, lesson) row; this stops one from silently clobbering another's result.
CREATE TABLE user_lessons (
    user_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_lessons PRIMARY KEY (user_id, lesson_id),
    CONSTRAINT fk_user_lessons_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_lessons_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
);

CREATE INDEX user_lessons_lesson_id_idx ON user_lessons (lesson_id);
