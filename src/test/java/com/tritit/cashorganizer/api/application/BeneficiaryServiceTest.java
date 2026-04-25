package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.Beneficiary;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.BeneficiaryPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BeneficiaryServiceTest {

    // CONSTANTES PARA LAS PRUEBAS
    private static final User USER = User.builder().id(UUID.randomUUID()).email("veiga@test.com").build();
    private static final Beneficiary BENEFICIARY = Beneficiary.builder().id(1L).name("Mercadona").user(USER).build();

    // DEFINICIÓN DE MOCKS
    private BeneficiaryPersistencePort persistencePortMock;
    private UserContextPort userContextPortMock;

    // SUJETO DE PRUEBA (SUT)
    private BeneficiaryService sut;

    @BeforeEach
    void setUp() {
        persistencePortMock = Mockito.mock(BeneficiaryPersistencePort.class);
        userContextPortMock = Mockito.mock(UserContextPort.class);
        sut = new BeneficiaryService(persistencePortMock, userContextPortMock);
        
        when(userContextPortMock.getCurrentUser()).thenReturn(USER);
    }

    @Test
    @DisplayName("getAllBeneficiaries: should return all beneficiaries for current user")
    void getAllBeneficiaries() {
        // Given
        when(persistencePortMock.findAllByUser(USER)).thenReturn(List.of(BENEFICIARY));

        // When
        List<Beneficiary> result = sut.getAllBeneficiaries();

        // Then
        assertThat(result).containsExactly(BENEFICIARY);
        verify(persistencePortMock, times(1)).findAllByUser(USER);
    }

    @Test
    @DisplayName("createBeneficiary: should set user and save beneficiary")
    void createBeneficiary() {
        // Given
        Beneficiary input = Beneficiary.builder().name("Nuevo").build();
        when(persistencePortMock.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Beneficiary result = sut.createBeneficiary(input);

        // Then
        assertThat(result.getUser()).isEqualTo(USER);
        assertThat(result.getName()).isEqualTo("Nuevo");
        verify(persistencePortMock, times(1)).save(any());
    }

    @Test
    @DisplayName("deleteBeneficiary: should delete if beneficiary belongs to user")
    void deleteBeneficiarySuccess() {
        // Given
        when(persistencePortMock.findById(1L)).thenReturn(Optional.of(BENEFICIARY));

        // When
        sut.deleteBeneficiary(1L);

        // Then
        verify(persistencePortMock, times(1)).delete(1L);
    }

    @Test
    @DisplayName("deleteBeneficiary: should not delete if beneficiary belongs to another user")
    void deleteBeneficiaryFail() {
        // Given
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Beneficiary otherBeneficiary = Beneficiary.builder().id(1L).user(otherUser).build();
        when(persistencePortMock.findById(1L)).thenReturn(Optional.of(otherBeneficiary));

        // When
        sut.deleteBeneficiary(1L);

        // Then
        verify(persistencePortMock, never()).delete(any());
    }
}
