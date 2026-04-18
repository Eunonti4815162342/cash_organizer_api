package com.tritit.cashorganizer.api.application.impact;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TransactionImpactResolver {

    private final Map<TransactionItem.TransactionType, TransactionImpact> strategies;

    public TransactionImpactResolver(List<TransactionImpact> impacts) {
        this.strategies = impacts.stream()
                .collect(Collectors.toMap(TransactionImpact::supportedType, Function.identity()));
    }

    public TransactionImpact forType(TransactionItem.TransactionType type) {
        TransactionItem.TransactionType effectiveType = type != null ? type : TransactionItem.TransactionType.EXPENSE;
        TransactionImpact impact = strategies.get(effectiveType);
        if (impact == null) {
            throw new IllegalStateException("No impact strategy registered for transaction type: " + effectiveType);
        }
        return impact;
    }
}
