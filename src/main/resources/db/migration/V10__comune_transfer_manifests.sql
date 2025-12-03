CREATE TABLE IF NOT EXISTS transfer_manifests (
                                                  id UUID PRIMARY KEY,
                                                  depot_id UUID NOT NULL REFERENCES depots(id),
    comune_name    TEXT NOT NULL,
    comune_contact TEXT,
    boxes_count    INT,
    seals_count    TEXT,
    prepared_by    TEXT,
    prepared_at    TIMESTAMPTZ DEFAULT now(),
    signed_by      TEXT,
    signed_at      TIMESTAMPTZ,
    signature_key  TEXT,
    status         TEXT NOT NULL DEFAULT 'OPEN'
    CHECK (status IN ('OPEN','SIGNED'))
    );

CREATE INDEX IF NOT EXISTS idx_transfer_manifests_depot
    ON transfer_manifests(depot_id);

CREATE INDEX IF NOT EXISTS idx_transfer_manifests_status
    ON transfer_manifests(status);


CREATE TABLE IF NOT EXISTS transfer_manifest_items (
                                                       id UUID PRIMARY KEY,
                                                       manifest_id UUID NOT NULL REFERENCES transfer_manifests(id) ON DELETE CASCADE,
    item_id     UUID NOT NULL REFERENCES items(id) ON DELETE RESTRICT,

    short_code  TEXT,
    category    TEXT,
    found_at    TIMESTAMPTZ,
    found_place TEXT,
    photo_key   TEXT
    );

CREATE INDEX IF NOT EXISTS idx_transfer_manifest_items_manifest
    ON transfer_manifest_items(manifest_id);

CREATE INDEX IF NOT EXISTS idx_transfer_manifest_items_item
    ON transfer_manifest_items(item_id);
