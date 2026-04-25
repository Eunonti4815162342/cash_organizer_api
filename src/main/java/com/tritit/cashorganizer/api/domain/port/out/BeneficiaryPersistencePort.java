package com.tritit.cashorganizer.api.domain.port.out;

import com.tritit.cashorganizer.api.domain.model.Beneficiary;
import com.tritit.cashorganizer.api.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface BeneficiaryPersistencePort {
    List<Beneficiary> findAllByUser(User user);
    Optional<Beneficiary> findById(Long id);
    Beneficiary save(Beneficiary beneficiary);
    void delete(Long id);
}
