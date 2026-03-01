package com.services.impl;

import com.dtos.PaymentDto;
import com.entities.Payment;
import com.enums.PaymentStatus;
import com.enums.PaymentType;
import com.enums.WalletProvider;
import com.mappers.PaymentMapper;
import com.repositories.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        paymentService = new PaymentServiceImpl(paymentRepository, new PaymentMapper(), validator);
    }

    @Test
    void savePayment_CartePayment() {
        PaymentDto request = cardDto(null, 20, "Jean Dupont", "4970100000000000", "12/30", "123");
        Payment persisted = cardEntity(10L, 20, "Jean Dupont", "4970100000000000", "12/30", "123");

        when(paymentRepository.save(any(Payment.class))).thenReturn(persisted);

        PaymentDto result = paymentService.savePayment(request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(PaymentType.CARTE, result.getType());
        assertEquals("123", result.getCvv());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void savePayment_CarteCvvMissing() {
        PaymentDto invalid = cardDto(null, 20, "Jean Dupont", "4970100000000000", "12/30", null);

        assertThrows(ConstraintViolationException.class, () -> paymentService.savePayment(invalid));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void savePayment_WalletProviderMissing() {
        PaymentDto invalid = walletDto(null, 25, null);

        assertThrows(ConstraintViolationException.class, () -> paymentService.savePayment(invalid));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void getPayment_NotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> paymentService.getPayment(99L));
    }

    @Test
    void deletePayment_NotFound() {
        when(paymentRepository.existsById(66L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> paymentService.deletePayment(66L));
    }

    private static PaymentDto cardDto(Long id, Integer montant, String owner,
                                      String number, String expiry, String cvv) {
        PaymentDto dto = new PaymentDto();
        dto.setId(id);
        dto.setType(PaymentType.CARTE);
        dto.setStatut(PaymentStatus.EN_ATTENTE);
        dto.setMontant(montant);
        dto.setCardOwner(owner);
        dto.setCardNumber(number);
        dto.setCardExpiryDate(expiry);
        dto.setCvv(cvv);
        return dto;
    }

    private static PaymentDto walletDto(Long id, Integer montant, WalletProvider provider) {
        PaymentDto dto = new PaymentDto();
        dto.setId(id);
        dto.setType(PaymentType.WALLET);
        dto.setStatut(PaymentStatus.EN_ATTENTE);
        dto.setMontant(montant);
        dto.setWalletProvider(provider);
        return dto;
    }

    private static Payment cardEntity(Long id, Integer montant, String owner,
                                      String number, String expiry,
                                      String cvv) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setType(PaymentType.CARTE);
        payment.setStatut(PaymentStatus.EN_ATTENTE);
        payment.setMontant(montant);
        payment.setCardOwner(owner);
        payment.setCardNumber(number);
        payment.setCardExpiryDate(expiry);
        payment.setCvv(cvv);
        return payment;
    }
}
