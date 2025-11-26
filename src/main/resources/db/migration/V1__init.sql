CREATE TABLE IF NOT EXISTS items (
                                     id UUID PRIMARY KEY,
                                     description TEXT NOT NULL,
                                     found_at TIMESTAMPTZ NOT NULL
);
