-- liquibase formatted sql

-- changeset Veiga:08-add-role-to-users splitStatements:true endDelimiter:;
-- Desc: Añadir columna de rol a usuarios para RBAC (INF-006)

ALTER TABLE cash_organizer.users ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'USER';
UPDATE cash_organizer.users SET role = 'USER' WHERE role IS NULL;
