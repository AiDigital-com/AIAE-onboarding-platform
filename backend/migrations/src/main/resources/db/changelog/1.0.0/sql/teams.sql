CREATE TABLE team_members (
    lead_user_id BIGINT NOT NULL,
    member_user_id BIGINT NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_team_members PRIMARY KEY (lead_user_id, member_user_id),
    CONSTRAINT fk_team_members_lead FOREIGN KEY (lead_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_member FOREIGN KEY (member_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT team_members_no_self_member CHECK (lead_user_id <> member_user_id)
);

CREATE INDEX team_members_member_user_id_idx ON team_members (member_user_id);
