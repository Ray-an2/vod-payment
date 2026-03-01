package com.mappers;

import com.dtos.PaymentDto;
import com.entities.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentDto toDto(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setId(payment.getId());
        paymentDto.setType(payment.getType());
        paymentDto.setStatut(payment.getStatut());
        paymentDto.setMontant(payment.getMontant());
        paymentDto.setWalletProvider(payment.getWalletProvider());
        paymentDto.setCardOwner(payment.getCardOwner());
        paymentDto.setCardNumber(payment.getCardNumber());
        paymentDto.setCardExpiryDate(payment.getCardExpiryDate());
        paymentDto.setCvv(payment.getCvv());
        return paymentDto;
    }

    public Payment toEntity(PaymentDto paymentDto) {
        if (paymentDto == null) {
            return null;
        }

        Payment payment = new Payment();
        if (paymentDto.getId() != null) {
            payment.setId(paymentDto.getId());
        }
        payment.setType(paymentDto.getType());
        payment.setStatut(paymentDto.getStatut());
        payment.setMontant(paymentDto.getMontant());
        payment.setWalletProvider(paymentDto.getWalletProvider());
        payment.setCardOwner(paymentDto.getCardOwner());
        payment.setCardNumber(paymentDto.getCardNumber());
        payment.setCardExpiryDate(paymentDto.getCardExpiryDate());
        payment.setCvv(paymentDto.getCvv());
        return payment;
    }
}
