package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportDataService {

    private final TransactionPersistencePort transactionPersistencePort;
    private final AccountPersistencePort accountPersistencePort;
    private final UserContextPort userContextPort;

    public Map<String, Long> getCategoryGroupedData(String startDate, String endDate, List<Long> accountIds, boolean groupBySubcategory) {
        User user = userContextPort.getCurrentUser();
        var transactions = (startDate != null && endDate != null)
                ? transactionPersistencePort.findAllByUserAndDateRange(user, startDate, endDate, Pageable.unpaged()).getContent()
                : transactionPersistencePort.findAllByUser(user, Pageable.unpaged()).getContent();

        return transactions.stream()
                .filter(t -> t.getCategory() != null)
                .filter(t -> accountIds == null || accountIds.isEmpty() || (t.getAccount() != null && accountIds.contains(t.getAccount().getId())))
                .collect(Collectors.groupingBy(
                        t -> (groupBySubcategory && t.getSubcategory() != null)
                             ? t.getCategory().getName() + " > " + t.getSubcategory().getName()
                             : t.getCategory().getName(),
                        Collectors.summingLong(t -> t.getAmount().getValue())
                ));
    }
}
