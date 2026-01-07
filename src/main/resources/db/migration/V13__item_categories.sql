-- 1) Items: main + sub category
ALTER TABLE items
    ADD COLUMN IF NOT EXISTS category_main TEXT NOT NULL DEFAULT 'MISC',
    ADD COLUMN IF NOT EXISTS category_sub  TEXT NOT NULL DEFAULT 'OTHER';

-- Backfill old rows (in case defaults didn't apply)
UPDATE items
SET category_main = COALESCE(category_main, 'MISC'),
    category_sub  = COALESCE(category_sub,  'OTHER')
WHERE category_main IS NULL OR category_sub IS NULL;

CREATE INDEX IF NOT EXISTS idx_items_category_main ON items(category_main);
CREATE INDEX IF NOT EXISTS idx_items_category_sub  ON items(category_sub);

-- 2) Transfer manifest lines: snapshot category at prepare time
ALTER TABLE transfer_manifest_items
    ADD COLUMN IF NOT EXISTS category_main TEXT;

-- NOTE: transfer_manifest_items already has "category" column.
-- We'll treat it as category_sub (no rename required).
CREATE INDEX IF NOT EXISTS idx_transfer_manifest_items_category_main
    ON transfer_manifest_items(category_main);
