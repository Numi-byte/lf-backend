CREATE TABLE IF NOT EXISTS transfer_manifest_pdfs (
                                                      id           UUID PRIMARY KEY,
                                                      manifest_id  UUID        NOT NULL REFERENCES transfer_manifests(id) ON DELETE CASCADE,
    lang         VARCHAR(8)  NOT NULL,
    pdf_data     BYTEA       NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    generated_by TEXT
    );

CREATE UNIQUE INDEX IF NOT EXISTS uk_transfer_manifest_pdfs_manifest_lang
    ON transfer_manifest_pdfs(manifest_id, lang);

CREATE INDEX IF NOT EXISTS idx_transfer_manifest_pdfs_manifest
    ON transfer_manifest_pdfs(manifest_id);