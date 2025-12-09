ALTER TABLE claims
    ADD COLUMN IF NOT EXISTS public_user_id TEXT,
    ADD COLUMN IF NOT EXISTS public_reference_code TEXT;

-- Optional but useful index for "my claims" lookup
CREATE INDEX IF NOT EXISTS idx_claims_public_user_email
    ON claims(public_user_id, passenger_email);
