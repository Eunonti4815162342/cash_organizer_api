package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.*;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class PersistenceMapper {

    private Long sanitizeId(Long id) {
        return (id != null && id != 0) ? id : null;
    }

    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .role(entity.getRole())
                .build();
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) return null;
        return UserEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .password(domain.getPassword())
                .role(domain.getRole())
                .build();
    }

    // --- ACCOUNT ---
    public AccountItem toDomain(AccountEntity entity) {
        if (entity == null) return null;
        return AccountItem.builder()
                .id(entity.getId())
                .user(toDomain(entity.getUser()))
                .name(entity.getName())
                .description(entity.getDescription())
                .accountType(entity.getAccountType())
                .flags(entity.getFlags())
                .notes(entity.getNotes())
                .accountOrder(entity.getAccountOrder())
                .active(entity.getActive())
                .amount(toDomain(entity.getAmount()))
                .entity(toDomain(entity.getFinancialEntity()))
                .build();
    }

    public AccountEntity toEntity(AccountItem domain) {
        if (domain == null) return null;
        return AccountEntity.builder()
                .id(sanitizeId(domain.getId()))
                .user(toEntity(domain.getUser()))
                .name(domain.getName())
                .description(domain.getDescription())
                .accountType(domain.getAccountType() != null ? domain.getAccountType() : "CASH")
                .flags(domain.getFlags() != null ? domain.getFlags() : 0)
                .notes(domain.getNotes())
                .accountOrder(domain.getAccountOrder() != null ? domain.getAccountOrder() : 0)
                .active(domain.getActive() != null ? domain.getActive() : true)
                .amount(toEntity(domain.getAmount()))
                .financialEntity(toEntity(domain.getEntity()))
                .build();
    }

    // --- AMOUNT ---
    public Amount toDomain(AmountEmbeddable entity) {
        if (entity == null) return null;
        return new Amount(entity.getValue(), entity.getCurrency(), entity.isNegative());
    }

    public AmountEmbeddable toEntity(Amount domain) {
        if (domain == null) return null;
        return new AmountEmbeddable(domain.getValue(), domain.getCurrency(), domain.isNegative());
    }

    // --- FINANCIAL ENTITY ---
    public FinancialEntity toDomain(FinancialEntityEntity entity) {
        if (entity == null) return null;
        return FinancialEntity.builder()
                .id(entity.getId())
                .user(toDomain(entity.getUser()))
                .name(entity.getName())
                .description(entity.getDescription())
                .country(entity.getCountry())
                .website(entity.getWebsite())
                .iconName(entity.getIconName())
                .build();
    }

    public FinancialEntityEntity toEntity(FinancialEntity domain) {
        if (domain == null) return null;
        return FinancialEntityEntity.builder()
                .id(sanitizeId(domain.getId()))
                .user(toEntity(domain.getUser()))
                .name(domain.getName())
                .description(domain.getDescription())
                .country(domain.getCountry())
                .website(domain.getWebsite())
                .iconName(domain.getIconName())
                .build();
    }

    // --- BENEFICIARY ---
    public Beneficiary toDomain(BeneficiaryEntity entity) {
        if (entity == null) return null;
        return Beneficiary.builder()
                .id(entity.getId())
                .user(toDomain(entity.getUser()))
                .name(entity.getName())
                .description(entity.getDescription())
                .financialEntity(toDomain(entity.getFinancialEntity()))
                .build();
    }

    public BeneficiaryEntity toEntity(Beneficiary domain) {
        if (domain == null) return null;
        return BeneficiaryEntity.builder()
                .id(sanitizeId(domain.getId()))
                .user(toEntity(domain.getUser()))
                .name(domain.getName())
                .description(domain.getDescription())
                .financialEntity(toEntity(domain.getFinancialEntity()))
                .build();
    }

    // --- CATEGORY ---
    public Category toDomain(CategoryEntity entity) {
        if (entity == null) return null;
        return Category.builder()
                .id(entity.getId())
                .user(toDomain(entity.getUser()))
                .name(entity.getName())
                .iconName(entity.getIconName())
                .type(entity.getType())
                .subcategories(entity.getSubcategories() != null ? 
                    entity.getSubcategories().stream().map(this::toDomain).collect(Collectors.toList()) : new ArrayList<>())
                .financialEntity(toDomain(entity.getFinancialEntity()))
                .build();
    }

    public Subcategory toDomain(SubcategoryEntity entity) {
        if (entity == null) return null;
        return Subcategory.builder()
                .id(entity.getId())
                .name(entity.getName())
                .iconName(entity.getIconName())
                .build();
    }

    public CategoryEntity toEntity(Category domain) {
        if (domain == null) return null;
        return CategoryEntity.builder()
                .id(sanitizeId(domain.getId()))
                .user(toEntity(domain.getUser()))
                .name(domain.getName())
                .iconName(domain.getIconName())
                .type(domain.getType())
                .financialEntity(toEntity(domain.getFinancialEntity()))
                .build();
    }

    // --- TRANSACTION ---
    public TransactionItem toDomain(TransactionItemEntity entity) {
        if (entity == null) return null;
        return TransactionItem.builder()
                .id(entity.getId())
                .user(toDomain(entity.getUser()))
                .date(entity.getDate())
                .description(entity.getDescription())
                .amount(toDomain(entity.getAmount()))
                .account(toDomain(entity.getAccount()))
                .category(toDomain(entity.getCategory()))
                .subcategory(toDomain(entity.getSubcategory()))
                .beneficiary(toDomain(entity.getBeneficiary()))
                .toAccount(toDomain(entity.getToAccount()))
                .type(entity.getType())
                .notes(entity.getNotes())
                .statusFlags(entity.getStatusFlags())
                .isScheduled(entity.getIsScheduled())
                .isHeader(entity.getIsHeader())
                .tags(entity.getTags())
                .build();
    }

    public TransactionItemEntity toEntity(TransactionItem domain) {
        if (domain == null) return null;
        return TransactionItemEntity.builder()
                .id(sanitizeId(domain.getId()))
                .user(toEntity(domain.getUser()))
                .date(domain.getDate())
                .description(domain.getDescription())
                .amount(toEntity(domain.getAmount()))
                .account(toEntity(domain.getAccount()))
                .category(toEntity(domain.getCategory()))
                .subcategory(toEntity(domain.getSubcategory()))
                .beneficiary(toEntity(domain.getBeneficiary()))
                .toAccount(toEntity(domain.getToAccount()))
                .type(domain.getType())
                .notes(domain.getNotes())
                .statusFlags(domain.getStatusFlags())
                .isScheduled(domain.getIsScheduled())
                .isHeader(domain.getIsHeader())
                .tags(domain.getTags())
                .build();
    }

    public SubcategoryEntity toEntity(Subcategory domain) {
        if (domain == null) return null;
        return SubcategoryEntity.builder()
                .id(sanitizeId(domain.getId()))
                .name(domain.getName())
                .iconName(domain.getIconName())
                .category(toEntity(domain.getCategory()))
                .build();
    }
}
