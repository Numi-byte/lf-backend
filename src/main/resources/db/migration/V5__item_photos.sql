CREATE TABLE IF NOT EXISTS item_photos (
                                           id UUID PRIMARY KEY,
                                           item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    object_key TEXT NOT NULL UNIQUE,
    content_type TEXT,
    size_bytes BIGINT,
    uploaded_at TIMESTAMPTZ DEFAULT now()
    );


CREATE INDEX IF NOT EXISTS idx_item_photos_item ON item_photos(item_id);