CREATE TABLE IF NOT EXISTS handovers (
                                         id UUID PRIMARY KEY,
                                         item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    depot_id UUID,                         -- snapshot of depot at handover time

-- who receives it / type of handover
    type TEXT NOT NULL DEFAULT 'PERSON' CHECK (type IN ('PERSON','COMUNE')),
    performed_by TEXT,                     -- user (agent) doing the handover

-- PERSON handover
    person_name TEXT,
    document_type TEXT,                    -- idcard | passport | other
    document_number TEXT,

    -- COMUNE handover
    comune_name TEXT,                      -- e.g. "Comune di Bolzano"
    comune_reference TEXT,                 -- protocol, note, reference number

-- general notes
    notes TEXT,

    -- photos for ID document (front/back), stored in S3/MinIO
    doc_front_key TEXT,
    doc_back_key  TEXT,

    -- fallback generic attachment if you ever need it
    attachment_key TEXT,

    created_at TIMESTAMPTZ DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_handovers_item ON handovers(item_id);
CREATE INDEX IF NOT EXISTS idx_handovers_depot ON handovers(depot_id);
CREATE INDEX IF NOT EXISTS idx_handovers_created_at ON handovers(created_at);
