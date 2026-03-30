-- Liquibase Changelog: Initial Schema for Cash Organizer (Simplified)
-- Las tablas se crean en el esquema por defecto configurado en la URL (cash_organizer)

-- 1. Table: Categories
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    icon_name VARCHAR(100),
    type VARCHAR(20) NOT NULL, -- EXPENSE, INCOME
    parent_id BIGINT,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

-- 2. Table: Accounts
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount_value BIGINT DEFAULT 0,
    amount_currency VARCHAR(10) DEFAULT 'USD',
    amount_is_negative BOOLEAN DEFAULT FALSE,
    description TEXT,
    account_type VARCHAR(50),
    flags INTEGER DEFAULT 0,
    notes TEXT,
    account_order INTEGER DEFAULT 0
);

-- 3. Table: Transactions
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    date_str VARCHAR(50), 
    description VARCHAR(255),
    amount_value BIGINT NOT NULL,
    amount_currency VARCHAR(10),
    amount_is_negative BOOLEAN,
    notes TEXT,
    status_flags INTEGER DEFAULT 0,
    is_scheduled BOOLEAN DEFAULT FALSE,
    is_header BOOLEAN DEFAULT FALSE,
    account_id BIGINT,
    category_id BIGINT,
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 4. Table: Transaction Tags
CREATE TABLE IF NOT EXISTS transaction_tags (
    transaction_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    CONSTRAINT fk_tags_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);
