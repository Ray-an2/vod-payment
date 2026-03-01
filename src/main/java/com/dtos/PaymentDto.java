package com.dtos;

import com.enums.PaymentStatus;
import com.enums.PaymentType;
import com.enums.WalletProvider;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PaymentDto {

    private Long id;

    @NotNull(message = "Le type est obligatoire")
    private PaymentType type;

    @NotNull(message = "Le statut est obligatoire")
    private PaymentStatus statut;

    @NotNull(message = "Le montant est obligatoire")
    @Min(value = 0, message = "Le montant doit etre superieur ou egal a 0")
    private Integer montant;

    private WalletProvider walletProvider;

    private String cardOwner;

    @Pattern(regexp = "^\\d{16}$", message = "Le numero de carte doit contenir 16 chiffres")
    private String cardNumber;

    @Pattern(regexp = "^(0[1-9]|1[0-2])\\/\\d{2}$", message = "La date de validite doit etre au format MM/YY")
    private String cardExpiryDate;

    @Pattern(regexp = "^\\d{3}$", message = "Le CVV doit contenir 3 chiffres")
    private String cvv;

    @AssertTrue(message = "Pour un paiement CARTE, cardOwner/cardNumber/cardExpiryDate/cvv sont obligatoires et walletProvider doit etre null")
    public boolean isCardPaymentDataValid() {
        if (type != PaymentType.CARTE) {
            return true;
        }

        return !isBlank(cardOwner)
                && !isBlank(cardNumber)
                && !isBlank(cardExpiryDate)
                && !isBlank(cvv)
                && walletProvider == null;
    }

    @AssertTrue(message = "Pour un paiement WALLET, walletProvider est obligatoire et les champs carte doivent etre vides")
    public boolean isWalletPaymentDataValid() {
        if (type != PaymentType.WALLET) {
            return true;
        }

        return walletProvider != null
                && isBlank(cardOwner)
                && isBlank(cardNumber)
                && isBlank(cardExpiryDate)
                && isBlank(cvv);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
