package com.services;

import com.dtos.PaymentDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface PaymentService {

    PaymentDto savePayment(@Valid @NotNull PaymentDto paymentDto);

    PaymentDto getPayment(Long paymentId);

    boolean deletePayment(Long paymentId);

    List<PaymentDto> getAllPayments();
}
