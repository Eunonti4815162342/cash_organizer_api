package com.tritit.cashorganizer.api;

import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            System.out.println(">> Synchronizing database schema...");
            
            // 1. Tabla Accounts
            jdbcTemplate.execute("ALTER TABLE cash_organizer.accounts ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;");
            
            // 2. Tabla Transactions
            jdbcTemplate.execute("ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS account_id BIGINT;");
            jdbcTemplate.execute("ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS category_id BIGINT;");
            jdbcTemplate.execute("ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS to_account_id BIGINT;");
            jdbcTemplate.execute("ALTER TABLE cash_organizer.transactions ADD COLUMN IF NOT EXISTS transaction_type VARCHAR(20);");
            
            System.out.println(">> Schema synchronization successful.");
        } catch (Exception e) {
            System.err.println(">> Warning during schema sync (might already be fixed): " + e.getMessage());
        }
    }
}
