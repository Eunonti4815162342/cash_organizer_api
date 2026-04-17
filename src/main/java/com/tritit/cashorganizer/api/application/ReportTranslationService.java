package com.tritit.cashorganizer.api.application;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportTranslationService {

    public String getLabel(String key, String lang) {
        Map<String, Map<String, String>> translations = new HashMap<>();

        Map<String, String> es = new HashMap<>();
        es.put("title", "INFORME FINANCIERO");
        es.put("balance", "SALDO ACTUAL");
        es.put("expenses", "GASTOS DEL PERIODO");
        es.put("date", "FECHA DEL INFORME");
        es.put("overview", "VISTA GENERAL DE CUENTAS");
        es.put("details", "DETALLE DE TRANSACCIONES");
        es.put("acc_name", "Nombre de Cuenta");
        es.put("acc_type", "Tipo");
        es.put("acc_bal", "Saldo");
        es.put("tx_date", "Fecha");
        es.put("tx_cat", "Categoría");
        es.put("tx_desc", "Descripción");
        es.put("tx_amt", "Importe");
        es.put("mo_inc", "Ingresos");
        es.put("mo_exp", "Gastos");
        es.put("mo_net", "Neto");
        translations.put("es", es);

        Map<String, String> pt = new HashMap<>();
        pt.put("title", "RELATÓRIO FINANCEIRO");
        pt.put("balance", "SALDO ATUAL");
        pt.put("expenses", "DESPESAS DO PERÍODO");
        pt.put("date", "DATA DO RELATÓRIO");
        pt.put("overview", "VISÃO GERAL DAS CONTAS");
        pt.put("details", "DETALHES DAS TRANSAÇÕES");
        pt.put("acc_name", "Nome da Conta");
        pt.put("acc_type", "Tipo");
        pt.put("acc_bal", "Saldo");
        pt.put("tx_date", "Data");
        pt.put("tx_cat", "Categoria");
        pt.put("tx_desc", "Descrição");
        pt.put("tx_amt", "Valor");
        pt.put("mo_inc", "Receitas");
        pt.put("mo_exp", "Despesas");
        pt.put("mo_net", "Líquido");
        translations.put("pt", pt);

        Map<String, String> en = new HashMap<>();
        en.put("title", "FINANCIAL REPORT");
        en.put("balance", "CURRENT BALANCE");
        en.put("expenses", "PERIOD EXPENSES");
        en.put("date", "REPORT DATE");
        en.put("overview", "ACCOUNTS OVERVIEW");
        en.put("details", "TRANSACTION DETAILS");
        en.put("acc_name", "Account Name");
        en.put("acc_type", "Type");
        en.put("acc_bal", "Balance");
        en.put("tx_date", "Date");
        en.put("tx_cat", "Category");
        en.put("tx_desc", "Description");
        en.put("tx_amt", "Amount");
        en.put("mo_inc", "Income");
        en.put("mo_exp", "Expenses");
        en.put("mo_net", "Net");
        translations.put("en", en);

        return translations.getOrDefault(lang.toLowerCase(), translations.get("en")).getOrDefault(key, key);
    }
}
