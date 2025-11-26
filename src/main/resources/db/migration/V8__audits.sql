CREATE TABLE IF NOT EXISTS audits (
                                      id UUID PRIMARY KEY,
                                      entity_type TEXT NOT NULL, -- ITEM | CLAIM | HANDOVER
                                      entity_id UUID NOT NULL,
                                      action TEXT NOT NULL,      -- ITEM_STORED | ITEM_HANDOVER | CLAIM_CREATED | ...
                                      actor TEXT,
                                      details TEXT,              -- JSON/text payload (before/after, etc.)
                                      at TIMESTAMPTZ DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_audits_entity ON audits(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audits_at ON audits(at);
