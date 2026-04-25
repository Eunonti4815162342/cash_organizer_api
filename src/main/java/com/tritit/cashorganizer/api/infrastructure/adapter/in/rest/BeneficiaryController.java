package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.BeneficiaryService;
import com.tritit.cashorganizer.api.application.TransactionService;
import com.tritit.cashorganizer.api.domain.model.Beneficiary;
import com.tritit.cashorganizer.api.domain.model.TransactionSuggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final TransactionService transactionService;

    @GetMapping
    public List<Beneficiary> getAll() {
        return beneficiaryService.getAllBeneficiaries();
    }

    @PostMapping
    public Beneficiary create(@RequestBody Beneficiary beneficiary) {
        return beneficiaryService.createBeneficiary(beneficiary);
    }

    @GetMapping("/{id}/suggestion")
    public TransactionSuggestion getSuggestion(@PathVariable Long id) {
        return transactionService.getSuggestionForBeneficiary(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        beneficiaryService.deleteBeneficiary(id);
    }
}
