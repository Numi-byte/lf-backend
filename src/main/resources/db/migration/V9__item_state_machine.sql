-- Map old states to new ones
UPDATE items SET state = 'REPORTED' WHERE state = 'in_intake';
UPDATE items SET state = 'SHELVED'  WHERE state = 'stored';
UPDATE items SET state = 'RETURNED' WHERE state = 'archived';

-- Default state is now REPORTED
ALTER TABLE items
    ALTER COLUMN state SET DEFAULT 'REPORTED';

-- Add a check constraint for the new state machine
ALTER TABLE items
DROP CONSTRAINT IF EXISTS chk_items_state;

ALTER TABLE items
    ADD CONSTRAINT chk_items_state
        CHECK (state IN (
                         'REPORTED',
                         'SHELVED',
                         'ON_HOLD',
                         'RETURNED',
                         'READY_FOR_TRANSFER',
                         'TRANSFERRED_TO_COMUNE'
            ));
