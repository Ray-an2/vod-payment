package com.entities;

import com.enums.PaymentStatus;
import com.enums.PaymentType;
import com.enums.WalletProvider;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    private PaymentStatus statut;

    private Integer montant;

    @Enumerated(EnumType.STRING)
    private WalletProvider walletProvider;

    private String cardOwner;
    private String cardNumber;
    private String cardExpiryDate;
    private String cvv;
}
