-- liquibase formatted sql

-- changeset Veiga:07-add-beneficiaries-table splitStatements:true endDelimiter:;
-- Desc: Creación de la tabla de beneficiarios y vinculación con transacciones

CREATE TABLE IF NOT EXISTS cash_organizer.beneficiaries (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    financial_entity_id BIGINT,
    CONSTRAINT fk_beneficiaries_user FOREIGN KEY (user_id) REFERENCES cash_organizer.users(id),
    CONSTRAINT fk_beneficiaries_financial_entity FOREIGN KEY (financial_entity_id) REFERENCES cash_organizer.financial_entities(id)
);

-- Añadir beneficiary_id a Transactions
ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS beneficiary_id BIGINT;
ALTER TABLE cash_organizer.transactions ADD CONSTRAINT fk_transactions_beneficiary FOREIGN KEY (beneficiary_id) REFERENCES cash_organizer.beneficiaries(id);

-- Índice para mejorar búsquedas
CREATE INDEX IF NOT EXISTS idx_beneficiaries_user ON cash_organizer.beneficiaries(user_id);
