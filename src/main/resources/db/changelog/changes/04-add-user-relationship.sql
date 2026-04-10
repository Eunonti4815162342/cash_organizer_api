-- liquibase formatted sql

-- changeset alvaro:04-add-user-relationship splitStatements:true endDelimiter:;
-- Desc: Vinculación de todas las entidades principales al esquema de usuarios para privacidad de datos

-- 1. Añadir user_id a Accounts
ALTER TABLE cash_organizer.accounts ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE cash_organizer.accounts ADD CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES cash_organizer.users(id);

-- 2. Añadir user_id a Transactions
ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE cash_organizer.transactions ADD CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES cash_organizer.users(id);

-- 3. Añadir user_id a Categories
ALTER TABLE cash_organizer.categories ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE cash_organizer.categories ADD CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES cash_organizer.users(id);

-- 4. Añadir user_id a Financial Entities
ALTER TABLE cash_organizer.financial_entities ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE cash_organizer.financial_entities ADD CONSTRAINT fk_entities_user FOREIGN KEY (user_id) REFERENCES cash_organizer.users(id);

-- 5. Crear índices para optimizar las búsquedas por usuario
CREATE INDEX IF NOT EXISTS idx_accounts_user ON cash_organizer.accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user ON cash_organizer.transactions(user_id);
