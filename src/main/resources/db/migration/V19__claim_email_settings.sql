CREATE TABLE IF NOT EXISTS claim_email_settings (
                                                    company TEXT PRIMARY KEY,
                                                    recipients TEXT NOT NULL,
                                                    claimant_subject_template TEXT NOT NULL,
                                                    claimant_body_template TEXT NOT NULL,
                                                    company_subject_template TEXT NOT NULL,
                                                    company_body_template TEXT NOT NULL,
                                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by TEXT
    );

INSERT INTO claim_email_settings (
    company,
    recipients,
    claimant_subject_template,
    claimant_body_template,
    company_subject_template,
    company_body_template,
    updated_by
)
VALUES
    ('sasa', 'sasabz@sasabz.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('sasabz', 'sasabz@sasabz.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('ksm', 'info@ksm.bz.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('pizzinini', 'info@pizzinini.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('taferner', 'info@taferner.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('autorainer', 'office@auto-rainer.com', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('auto-rainer', 'office@auto-rainer.com', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('simobil', 'info@silbernagl.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('silbernagl', 'info@silbernagl.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('kronplatz', 'mobility@kronplatz.group', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('sad', 'info@sad.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('holzer', 'info@holzer.eu', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('trenitalia', 'customer.room.bolzano@trenitalia.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration'),
    ('sta', 'infopoint-bz@sta.bz.it', 'Your lost-and-found claim {claimReference} was submitted', $claimant$Dear {passengerName},

     Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.

Claim reference: {claimReference}
Item: {itemDescription}
Submitted at: {submittedAt}

Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.

Kind regards,
     Lost & Found Team$claimant$, 'New lost-and-found claim received: {claimReference}', $company$A new lost-and-found claim has been received for {company}.

Claim reference: {claimReference}
Status: {status}
Submitted at: {submittedAt}

Claimant details
Name: {passengerName}
Email: {passengerEmail}
Phone: {passengerPhone}

Item details
Item ID: {itemId}
Description: {itemDescription}

Claim narrative
{narrative}

Please review the claim in the Lost & Found back office.$company$, 'migration')
    ON CONFLICT (company) DO NOTHING;