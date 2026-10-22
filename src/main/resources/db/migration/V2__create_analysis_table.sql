CREATE TABLE analyses(
    id uuid PRIMARY KEY default gen_random_uuid(),
    user_id BIGINT NOT NULL
                     REFERENCES users (id) ON DELETE CASCADE ,
    project_name TEXT NOT NULL,
    json_analysis jsonb NOT NULL,
    source_code jsonb NOT NULL,
    source TEXT NOT NULL,
    github_url TEXT,
    CONSTRAINT chk_source CHECK ( source IN ('GITHUB', 'ZIP')),
    created_at timestamptz NOT NULL default now(),
    CONSTRAINT chk_source_then_github_url_needed CHECK
        (source <> 'GITHUB' OR github_url IS NOT NULL)
);


CREATE INDEX idx_analyses_user_id_created_at ON analyses (user_id, created_at DESC );