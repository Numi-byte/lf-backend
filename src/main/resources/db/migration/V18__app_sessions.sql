CREATE TABLE IF NOT EXISTS app_sessions (
                                            token TEXT PRIMARY KEY,
                                            user_id TEXT NOT NULL,
                                            email TEXT,
                                            role TEXT NOT NULL,
                                            company TEXT NOT NULL,
                                            expires_at TIMESTAMPTZ NOT NULL,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_app_sessions_expires_at ON app_sessions(expires_at);