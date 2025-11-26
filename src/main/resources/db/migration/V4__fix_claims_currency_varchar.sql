-- Make currency compatible with JPA String length mapping
ALTER TABLE claims
ALTER COLUMN currency TYPE varchar(3),
  ALTER COLUMN currency SET DEFAULT 'EUR';

-- (Optional, but nice) enforce exactly 3 characters
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'chk_claims_currency_len'
      AND conrelid = 'claims'::regclass
  ) THEN
ALTER TABLE claims
    ADD CONSTRAINT chk_claims_currency_len
        CHECK (char_length(currency) = 3);
END IF;
END$$;
