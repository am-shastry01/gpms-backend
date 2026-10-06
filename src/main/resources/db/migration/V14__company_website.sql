/*
 * The gate pass footer carries the company's website beside its email
 * and phone, and there was nowhere to put one.
 */

ALTER TABLE company_profile
    ADD COLUMN IF NOT EXISTS website VARCHAR(200);
