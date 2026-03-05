CREATE TABLE IF NOT EXISTS catalog_visibility_rules (
                                                        id BIGSERIAL PRIMARY KEY,
                                                        main_code TEXT NOT NULL,
                                                        sub_code TEXT NULL,
                                                        CONSTRAINT uq_catalog_visibility_main_sub UNIQUE (main_code, sub_code)
    );

CREATE INDEX IF NOT EXISTS idx_catalog_visibility_main ON catalog_visibility_rules(main_code);