package com.tritit.cashorganizer.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@Configuration
public class DatabaseFixConfig {

    @Bean
    public boolean fixDatabaseSchema(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            System.out.println(">> [CRITICAL] Starting comprehensive schema repair...");
            
            // 1. Crear Tabla de Entidades Financieras
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS cash_organizer.financial_entities (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "tax_id VARCHAR(50), " +
                    "description TEXT, " +
                    "type VARCHAR(20) NOT NULL" +
                    ");");

            // 2. Reparar Tabla Accounts
            jdbcTemplate.execute("ALTER TABLE cash_organizer.accounts ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;");
            jdbcTemplate.execute("ALTER TABLE cash_organizer.accounts ADD COLUMN IF NOT EXISTS entity_id BIGINT;");
            
            // 3. Reparar Tabla Transactions
            jdbcTemplate.execute("ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS account_id BIGINT;");
            jdbcTemplate.execute("ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS category_id BIGINT;");
            jdbcTemplate.execute("ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS to_account_id BIGINT;");
            jdbcTemplate.execute("ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS transaction_type VARCHAR(255);");
            
            // 4. Eliminar restricciones antiguas si existen
            jdbcTemplate.execute("ALTER TABLE cash_organizer.transactions DROP CONSTRAINT IF EXISTS transactions_transaction_type_check;");
            
            System.out.println(">> [SUCCESS] Database schema synchronized with Entities support.");
        } catch (Exception e) {
            System.err.println(">> [WARNING] Schema repair note: " + e.getMessage());
        }
        return true;
    }
}
