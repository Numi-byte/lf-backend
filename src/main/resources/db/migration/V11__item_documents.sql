CREATE TABLE IF NOT EXISTS item_documents (
                                              id UUID PRIMARY KEY,
                                              item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,

    doc_type TEXT NOT NULL,       -- e.g. 'ITALIAN_ID'
    doc_name TEXT,                -- name on the document
    doc_birthdate DATE,           -- YYYY-MM-DD
    doc_issuer TEXT,              -- e.g. 'Comune di Bolzano'
    doc_number_full TEXT,         -- e.g. 'AA12345BB' (internal only)
    doc_match_hash TEXT NOT NULL  -- hash used for matching (digits + DOB, etc.)
    );

CREATE INDEX IF NOT EXISTS idx_item_documents_item
    ON item_documents(item_id);

CREATE INDEX IF NOT EXISTS idx_item_documents_hash
    ON item_documents(doc_match_hash);
