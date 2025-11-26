CREATE TABLE IF NOT EXISTS claims (
                                      id UUID PRIMARY KEY,
                                      item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    passenger_name TEXT,
    passenger_email TEXT,
    passenger_phone TEXT,
    narrative TEXT,
    status TEXT NOT NULL DEFAULT 'new' CHECK (status IN ('new','in_review','approved','rejected','closed')),
    method TEXT CHECK (method IN ('pickup','ship')),
    fee_cents INT DEFAULT 0,
    currency CHAR(3) DEFAULT 'EUR',
    submitted_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
    );


CREATE INDEX IF NOT EXISTS idx_claims_item ON claims(item_id);