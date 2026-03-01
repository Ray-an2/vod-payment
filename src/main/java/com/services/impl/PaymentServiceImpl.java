package com.services.impl;

import com.dtos.PaymentDto;
import com.mappers.PaymentMapper;
import com.repositories.PaymentRepository;
import com.services.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

@Service("paymentService")
@Transactional
@Validated
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final Validator validator;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentMapper paymentMapper,
                              Validator validator) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.validator = validator;
    }

    @Override
    public PaymentDto savePayment(@Valid PaymentDto paymentDto) {
        validatePayment(Objects.requireNonNull(paymentDto, "paymentDto ne doit pas etre null"));
        var payment = paymentMapper.toEntity(paymentDto);
        var savedPayment = paymentRepository.save(payment);
        return paymentMapper.toDto(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(Long paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Le paiement avec l'ID %d n'existe pas", paymentId)));
        return paymentMapper.toDto(payment);
    }

    @Override
    public boolean deletePayment(Long paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new EntityNotFoundException(
                    String.format("Le paiement avec l'ID %d n'existe pas", paymentId));
        }
        paymentRepository.deleteById(paymentId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDto> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    private void validatePayment(PaymentDto paymentDto) {
        var violations = validator.validate(paymentDto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
