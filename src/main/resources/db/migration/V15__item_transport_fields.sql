ALTER TABLE items
    ADD COLUMN IF NOT EXISTS transport_type TEXT,
    ADD COLUMN IF NOT EXISTS transport_line TEXT,
    ADD COLUMN IF NOT EXISTS transport_line_de TEXT;

CREATE INDEX IF NOT EXISTS idx_items_transport_type ON items(transport_type);
CREATE INDEX IF NOT EXISTS idx_items_transport_line ON items(transport_line);