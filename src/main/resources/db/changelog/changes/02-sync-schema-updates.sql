-- liquibase formatted sql

-- changeset alvaro:02-sync-schema-from-datainitializer splitStatements:false
-- Desc: Syncing manual changes from DataInitializer to Liquibase

-- 1. Table Accounts
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='cash_organizer' AND table_name='accounts' AND column_name='active') THEN
        ALTER TABLE cash_organizer.accounts ADD COLUMN active BOOLEAN DEFAULT TRUE;
    END IF;
END $$;
UPDATE cash_organizer.accounts SET active = TRUE WHERE active IS NULL;

-- 2. Table Transactions
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='cash_organizer' AND table_name='transactions' AND column_name='account_id') THEN
        ALTER TABLE cash_organizer.transactions ADD COLUMN account_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='cash_organizer' AND table_name='transactions' AND column_name='category_id') THEN
        ALTER TABLE cash_organizer.transactions ADD COLUMN category_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='cash_organizer' AND table_name='transactions' AND column_name='to_account_id') THEN
        ALTER TABLE cash_organizer.transactions ADD COLUMN to_account_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='cash_organizer' AND table_name='transactions' AND column_name='transaction_type') THEN
        ALTER TABLE cash_organizer.transactions ADD COLUMN transaction_type VARCHAR(255);
    END IF;
END $$;

-- 2.1 Table Accounts Extras
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='cash_organizer' AND table_name='accounts' AND column_name='entity_id') THEN
        ALTER TABLE cash_organizer.accounts ADD COLUMN entity_id BIGINT;
    END IF;
END $$;

-- 4. Financial Entities Table
CREATE TABLE IF NOT EXISTS cash_organizer.financial_entities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50),
    description TEXT,
    type VARCHAR(20) NOT NULL
);

-- 5. Cleanup
ALTER TABLE cash_organizer.transactions DROP CONSTRAINT IF EXISTS transactions_transaction_type_check;

-- 3. Users table (for security system)
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);
