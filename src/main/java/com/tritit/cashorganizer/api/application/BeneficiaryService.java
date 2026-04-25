package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.Beneficiary;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.BeneficiaryPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryPersistencePort beneficiaryPersistencePort;
    private final UserContextPort userContextPort;

    @Transactional(readOnly = true)
    public List<Beneficiary> getAllBeneficiaries() {
        User user = userContextPort.getCurrentUser();
        return beneficiaryPersistencePort.findAllByUser(user);
    }

    @Transactional
    public Beneficiary createBeneficiary(Beneficiary beneficiary) {
        User user = userContextPort.getCurrentUser();
        beneficiary.setUser(user);
        return beneficiaryPersistencePort.save(beneficiary);
    }

    @Transactional
    public void deleteBeneficiary(Long id) {
        User user = userContextPort.getCurrentUser();
        beneficiaryPersistencePort.findById(id)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .ifPresent(b -> beneficiaryPersistencePort.delete(id));
    }
}
