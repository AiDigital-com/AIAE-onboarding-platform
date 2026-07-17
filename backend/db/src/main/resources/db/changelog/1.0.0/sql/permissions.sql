CREATE TABLE user_permission_overrides (
    user_id BIGINT NOT NULL,
    permission_key TEXT NOT NULL,
    is_allowed BOOLEAN NOT NULL,
    granted_by_user_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_permission_overrides PRIMARY KEY (user_id, permission_key),
    CONSTRAINT fk_upo_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_upo_granted_by FOREIGN KEY (granted_by_user_id) REFERENCES users (id) ON DELETE SET NULL
);
