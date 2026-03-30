package com.tritit.cashorganizer.api;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        // Silenciamos temporalmente para permitir que Liquibase cree las tablas sin errores de inserción
        System.out.println(">> DataInitializer silenciado temporalmente para depuración de Liquibase.");
        
        /*
        // Cuentas de prueba
        AccountItem acc1 = new AccountItem(null, "Efectivo", new Amount(50000, "USD", false), "Billetera", "Cash", 1, "Notas de prueba", 1);
        AccountItem acc2 = new AccountItem(null, "Banco Principal", new Amount(1250000, "USD", false), "Cuenta ahorros", "Bank", 2, "", 2);
        accountRepository.saveAll(List.of(acc1, acc2));

        // Categorías de Gasto
        Category food = new Category(null, "Alimentación", "ic_cat_food", Category.CategoryType.EXPENSE, null, new ArrayList<>());
        Category transport = new Category(null, "Transporte", "ic_cat_transport", Category.CategoryType.EXPENSE, null, new ArrayList<>());
        categoryRepository.saveAll(List.of(food, transport));
        
        // Subcategorías
        Category restaurant = new Category(null, "Restaurantes", "ic_cat_restaurant", Category.CategoryType.EXPENSE, food, null);
        categoryRepository.save(restaurant);

        // Categorías de Ingreso
        Category salary = new Category(null, "Salario", "ic_cat_salary", Category.CategoryType.INCOME, null, new ArrayList<>());
        categoryRepository.save(salary);

        // Transacciones de prueba
        TransactionItem t1 = new TransactionItem(null, "2026-03-29", "Compra Supermercado", new Amount(4500, "USD", true), "Comida semanal", 0, false, false, List.of("Alimentos", "Hogar"));
        TransactionItem t2 = new TransactionItem(null, "2026-03-28", "Nómina Marzo", new Amount(250000, "USD", false), "Salario mensual", 0, false, false, List.of("Ingresos"));
        transactionRepository.saveAll(List.of(t1, t2));
        */
    }
}