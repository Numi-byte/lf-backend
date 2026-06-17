CREATE TABLE IF NOT EXISTS customer_email_otps (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   email TEXT NOT NULL,
                                                   code_hash TEXT NOT NULL,
                                                   device_hash TEXT NOT NULL,
                                                   attempts_remaining INTEGER NOT NULL,
                                                   expires_at TIMESTAMPTZ NOT NULL,
                                                   consumed_at TIMESTAMPTZ,
                                                   created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_customer_email_otps_email_created_at
    ON customer_email_otps(email, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_customer_email_otps_expires_at
    ON customer_email_otps(expires_at);