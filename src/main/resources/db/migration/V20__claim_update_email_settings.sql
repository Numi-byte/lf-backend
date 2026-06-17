ALTER TABLE claim_email_settings
    ADD COLUMN IF NOT EXISTS claimant_update_subject_template TEXT,
    ADD COLUMN IF NOT EXISTS claimant_update_body_template TEXT,
    ADD COLUMN IF NOT EXISTS company_update_subject_template TEXT,
    ADD COLUMN IF NOT EXISTS company_update_body_template TEXT;

UPDATE claim_email_settings
SET claimant_update_subject_template = COALESCE(claimant_update_subject_template, 'Your lost-and-found claim {claimReference} was updated'),
    claimant_update_body_template = COALESCE(claimant_update_body_template, $claimant_update$Dear {passengerName},

There is an update for your lost-and-found claim.

Claim reference: {claimReference}
Item: {itemDescription}
Previous status: {previousStatus}
Current status: {status}
Previous item status: {previousItemState}
Current item status: {itemState}
Updated at: {updatedAt}

We will contact you if we need more information.

Kind regards,
Lost & Found Team$claimant_update$),
    company_update_subject_template = COALESCE(company_update_subject_template, 'Lost-and-found claim updated: {claimReference}'),
    company_update_body_template = COALESCE(company_update_body_template, $company_update$A lost-and-found claim for {company} has been updated.

Claim reference: {claimReference}
Previous status: {previousStatus}
Current status: {status}
Previous item status: {previousItemState}
Current item status: {itemState}
Updated at: {updatedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Please review the claim in the Lost & Found back office.$company_update$);

ALTER TABLE claim_email_settings
    ALTER COLUMN claimant_update_subject_template SET NOT NULL,
ALTER COLUMN claimant_update_body_template SET NOT NULL,
    ALTER COLUMN company_update_subject_template SET NOT NULL,
    ALTER COLUMN company_update_body_template SET NOT NULL;