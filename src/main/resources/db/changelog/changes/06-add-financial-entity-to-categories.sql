-- liquibase formatted sql

-- changeset Veiga:06-add-financial-entity-to-categories splitStatements:true endDelimiter:;
-- Desc: Asociar categorías a entidades financieras (CAT-001)

ALTER TABLE cash_organizer.categories ADD COLUMN IF NOT EXISTS financial_entity_id BIGINT;
ALTER TABLE cash_organizer.categories ADD CONSTRAINT fk_categories_financial_entity FOREIGN KEY (financial_entity_id) REFERENCES cash_organizer.financial_entities(id);

CREATE INDEX IF NOT EXISTS idx_categories_financial_entity ON cash_organizer.categories(financial_entity_id);
