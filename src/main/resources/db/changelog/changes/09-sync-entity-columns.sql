-- Sync schema with the current entity model (definitions taken from the dev DB,
-- which ddl-auto: update kept aligned while Liquibase was inactive after the
-- Boot 4 upgrade). Old columns (amount_value, date_str, transaction_type...)
-- are kept: they carry no data outside dev and validate ignores extras.

ALTER TABLE accounts ADD COLUMN IF NOT EXISTS currency VARCHAR(255);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS is_negative BOOLEAN;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS value BIGINT;

ALTER TABLE financial_entities ADD COLUMN IF NOT EXISTS country VARCHAR(255);
ALTER TABLE financial_entities ADD COLUMN IF NOT EXISTS icon_name VARCHAR(255);
ALTER TABLE financial_entities ADD COLUMN IF NOT EXISTS website VARCHAR(255);

CREATE TABLE IF NOT EXISTS subcategories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    icon_name VARCHAR(255),
    category_id BIGINT,
    CONSTRAINT fk_subcategory_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS currency VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS is_negative BOOLEAN;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS value BIGINT;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS "date" VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS type VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS subcategory_id BIGINT;
