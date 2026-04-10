-- liquibase formatted sql

-- changeset alvaro:03-create-users-table splitStatements:true endDelimiter:;
-- Desc: Creación de la tabla de usuarios en el esquema cash_organizer para el sistema de seguridad JWT

CREATE SCHEMA IF NOT EXISTS cash_organizer;

CREATE TABLE IF NOT EXISTS cash_organizer.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);
