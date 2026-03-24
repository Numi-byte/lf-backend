ALTER TABLE depots
    ADD COLUMN IF NOT EXISTS company TEXT NOT NULL DEFAULT 'sta';

ALTER TABLE items
    ADD COLUMN IF NOT EXISTS company TEXT NOT NULL DEFAULT 'sta';

UPDATE depots
SET company = 'sta'
WHERE company IS NULL OR btrim(company) = '';

UPDATE items i
SET company = d.company
    FROM locations l
         JOIN depots d ON d.id = l.depot_id
WHERE i.current_location_id = l.id;

UPDATE items
SET company = 'sta'
WHERE company IS NULL OR btrim(company) = '';

CREATE INDEX IF NOT EXISTS idx_depots_company ON depots(company);
CREATE INDEX IF NOT EXISTS idx_items_company ON items(company);