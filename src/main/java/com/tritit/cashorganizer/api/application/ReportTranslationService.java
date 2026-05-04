package com.tritit.cashorganizer.api.application;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportTranslationService {

    public String getLabel(String key, String lang) {
        Map<String, Map<String, String>> translations = new HashMap<>();

        Map<String, String> es = new HashMap<>();
        es.put("title", "INFORME DE AUDITORÍA NATAVE");
        es.put("balance", "SALDO ACTUAL");
        es.put("expenses", "GASTOS DEL PERIODO");
        es.put("date", "FECHA DEL INFORME");
        es.put("overview", "RESUMEN GENERAL DE GASTOS");
        es.put("details", "DETALLE DE TRANSACCIONES");
        es.put("acc_name", "Cuenta");
        es.put("acc_type", "Tipo");
        es.put("acc_bal", "Saldo");
        es.put("tx_date", "Fecha");
        es.put("tx_cat", "Categoría");
        es.put("tx_desc", "Descripción");
        es.put("tx_amt", "Importe");
        es.put("mo_inc", "Ingresos");
        es.put("mo_exp", "Gastos");
        es.put("mo_net", "Neto");
        es.put("no_data", "No hay movimientos para los filtros seleccionados.");
        es.put("period_label", "PERIODO");
        es.put("company", "EMPRESA");
        es.put("beneficiary", "Beneficiario");
        es.put("total_income", "TOTAL INGRESOS EMPRESA");
        es.put("total_expense", "TOTAL GASTOS EMPRESA");
        es.put("total_incomes", "TOTAL INGRESOS");
        es.put("total_expenses", "TOTAL GASTOS");
        translations.put("es", es);

        Map<String, String> pt = new HashMap<>();
        pt.put("title", "RELATÓRIO DE AUDITORIA NATAVE");
        pt.put("balance", "SALDO ATUAL");
        pt.put("expenses", "DESPESAS DO PERÍODO");
        pt.put("date", "DATA DO RELATÓRIO");
        pt.put("overview", "RESUMO GERAL DE DESPESAS");
        pt.put("details", "DETALHES DAS TRANSAÇÕES");
        pt.put("acc_name", "Conta");
        pt.put("acc_type", "Tipo");
        pt.put("acc_bal", "Saldo");
        pt.put("tx_date", "Data");
        pt.put("tx_cat", "Categoria");
        pt.put("tx_desc", "Descrição");
        pt.put("tx_amt", "Valor");
        pt.put("mo_inc", "Receitas");
        pt.put("mo_exp", "Despesas");
        pt.put("mo_net", "Líquido");
        pt.put("no_data", "Não há movimentos para os filtros selecionados.");
        pt.put("period_label", "PERÍODO");
        pt.put("company", "EMPRESA");
        pt.put("beneficiary", "Beneficiário");
        pt.put("total_income", "TOTAL DE RECEITAS DA EMPRESA");
        pt.put("total_expense", "TOTAL DE DESPESAS DA EMPRESA");
        pt.put("total_incomes", "TOTAL RECEITAS");
        pt.put("total_expenses", "TOTAL DESPESAS");
        translations.put("pt", pt);

        Map<String, String> en = new HashMap<>();
        en.put("title", "NATAVE AUDIT REPORT");
        en.put("balance", "CURRENT BALANCE");
        en.put("expenses", "PERIOD EXPENSES");
        en.put("date", "REPORT DATE");
        en.put("overview", "GENERAL EXPENSE SUMMARY");
        en.put("details", "TRANSACTION DETAILS");
        en.put("acc_name", "Account");
        en.put("acc_type", "Type");
        en.put("acc_bal", "Balance");
        en.put("tx_date", "Date");
        en.put("tx_cat", "Category");
        en.put("tx_desc", "Description");
        en.put("tx_amt", "Amount");
        en.put("mo_inc", "Income");
        en.put("mo_exp", "Expenses");
        en.put("mo_net", "Net");
        en.put("no_data", "No movements found for the selected filters.");
        en.put("period_label", "PERIOD");
        en.put("company", "COMPANY");
        en.put("beneficiary", "Beneficiary");
        en.put("total_income", "TOTAL COMPANY INCOME");
        en.put("total_expense", "TOTAL COMPANY EXPENSES");
        en.put("total_incomes", "TOTAL INCOME");
        en.put("total_expenses", "TOTAL EXPENSES");
        translations.put("en", en);

        return translations.getOrDefault(lang.toLowerCase(), translations.get("en")).getOrDefault(key, key);
    }
}
