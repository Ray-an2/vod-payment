package com.controllers;

import com.dtos.PaymentDto;
import com.enums.PaymentStatus;
import com.enums.PaymentType;
import com.enums.WalletProvider;
import com.services.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(PaymentControllerTest.MockConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentService paymentService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        PaymentService paymentService() {
            return mock(PaymentService.class);
        }
    }

    /**
     * @Description: fabrique un PaymentDto pour eviter de la duplication dans les tests.
     * @Params:
     * - id: identifiant du paiement (null en creation).
     * - type: type de paiement (CARTE ou WALLET).
     * - status: statut du paiement.
     * - montant: montant du paiement.
     * - wallet: fournisseur (PAYPAL/APPLE_PAY/GOOGLE_PAY), obligatoire si type=WALLET.
     * - cardOwner: proprietaire de la carte, obligatoire si type=CARTE.
     * - cardNumber: numero de carte (16 chiffres), obligatoire si type=CARTE.
     * - cardExpiryDate: date validite carte au format MM/YY, obligatoire si type=CARTE.
     * - cvv: code CVV a 3 chiffres, obligatoire si type=CARTE.
     * @Return: un PaymentDto initialise avec les valeurs passees en parametre.
     */
    private static PaymentDto paymentDto(Long id, PaymentType type, PaymentStatus status,
                                         Integer montant,
                                         WalletProvider wallet,
                                         String cardOwner, String cardNumber,
                                         String cardExpiryDate, String cvv) {
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setId(id);
        paymentDto.setType(type);
        paymentDto.setStatut(status);
        paymentDto.setMontant(montant);
        paymentDto.setWalletProvider(wallet);
        paymentDto.setCardOwner(cardOwner);
        paymentDto.setCardNumber(cardNumber);
        paymentDto.setCardExpiryDate(cardExpiryDate);
        paymentDto.setCvv(cvv);
        return paymentDto;
    }

    /**
     * @Description: verifie que la requete GET renvoie une liste de paiements quand des donnees existent.
     * @Return: HTTP 200 + tableau JSON contenant les paiements 1 et 2.
     */
    @Test
    void getPayments() throws Exception {
        PaymentDto p1 = paymentDto(1L, PaymentType.CARTE, PaymentStatus.EFFECTUE, 12,
                null, "Jean Dupont", "4970100000000000", "12/30", "123");
        PaymentDto p2 = paymentDto(2L, PaymentType.WALLET, PaymentStatus.EN_ATTENTE, 20,
                WalletProvider.PAYPAL, null, null, null, null);

        when(paymentService.getAllPayments()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("CARTE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].type").value("WALLET"))
                .andExpect(jsonPath("$[1].walletProvider").value("PAYPAL"));
    }

    /**
     * @Description: verifie que la requete GET renvoie une liste vide quand aucun paiement n'existe.
     * @Return: HTTP 200 + tableau JSON vide [].
     */
    @Test
    void getPayments_EmptyList() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(List.of());

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    /**
     * @Description: verifie que la requete GET renvoie le paiement correspondant si il existe.
     * @Return: HTTP 200 + JSON du paiement id=5.
     */
    @Test
    void getPayment() throws Exception {
        PaymentDto payment = paymentDto(5L, PaymentType.WALLET, PaymentStatus.EFFECTUE, 30,
                WalletProvider.APPLE_PAY, null, null, null, null);
        when(paymentService.getPayment(5L)).thenReturn(payment);

        mockMvc.perform(get("/payments/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.type").value("WALLET"))
                .andExpect(jsonPath("$.walletProvider").value("APPLE_PAY"))
                .andExpect(jsonPath("$.statut").value("EFFECTUE"))
                .andExpect(jsonPath("$.montant").value(30));
    }

    /**
     * @Description: verifie que la requete GET retourne une erreur 404 si le paiement n'existe pas.
     * @Return: HTTP 404 + message d'erreur.
     */
    @Test
    void getPayment_NotFound() throws Exception {
        when(paymentService.getPayment(99L))
                .thenThrow(new EntityNotFoundException("Le paiement avec l'ID 99 n'existe pas"));

        mockMvc.perform(get("/payments/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Le paiement avec l'ID 99 n'existe pas"));
    }

    /**
     * @Description: verifie que la requete POST cree un paiement CARTE valide.
     * @Return: HTTP 201 + JSON du paiement cree (id=10).
     */
    @Test
    void savePayment_CardValid() throws Exception {
        PaymentDto toCreate = paymentDto(null, PaymentType.CARTE, PaymentStatus.EN_ATTENTE, 15,
                null, "Jean Dupont", "4970100000000000", "12/30", "123");
        PaymentDto created = paymentDto(10L, PaymentType.CARTE, PaymentStatus.EN_ATTENTE, 15,
                null, "Jean Dupont", "4970100000000000", "12/30", "123");

        when(paymentService.savePayment(any(PaymentDto.class))).thenReturn(created);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("CARTE"))
                .andExpect(jsonPath("$.cardNumber").value("4970100000000000"))
                .andExpect(jsonPath("$.cvv").value("123"));
    }

    /**
     * @Description: verifie que la requete POST cree un paiement WALLET valide.
     * @Return: HTTP 201 + JSON du paiement wallet cree.
     */
    @Test
    void savePayment_WalletValid() throws Exception {
        PaymentDto toCreate = paymentDto(null, PaymentType.WALLET, PaymentStatus.EN_ATTENTE, 25,
                WalletProvider.PAYPAL, null, null, null, null);
        PaymentDto created = paymentDto(11L, PaymentType.WALLET, PaymentStatus.EN_ATTENTE, 25,
                WalletProvider.PAYPAL, null, null, null, null);

        when(paymentService.savePayment(any(PaymentDto.class))).thenReturn(created);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.type").value("WALLET"))
                .andExpect(jsonPath("$.walletProvider").value("PAYPAL"));
    }

    /**
     * @Description: verifie que la requete POST accepte un id fourni dans le payload.
     * @Return: HTTP 201 + JSON avec id conserve.
     */
    @Test
    void savePayment_WithIdProvided() throws Exception {
        PaymentDto toCreate = paymentDto(77L, PaymentType.CARTE, PaymentStatus.EN_ATTENTE, 18,
                null, "Jean Dupont", "4970100000000000", "12/30", "123");
        PaymentDto created = paymentDto(77L, PaymentType.CARTE, PaymentStatus.EN_ATTENTE, 18,
                null, "Jean Dupont", "4970100000000000", "12/30", "123");

        when(paymentService.savePayment(any(PaymentDto.class))).thenReturn(created);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(77));
    }

    /**
     * @Description: verifie que la requete POST retourne 400 si type est absent.
     * @Return: HTTP 400 (type obligatoire).
     */
    @Test
    void savePayment_MissingType() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "statut", "EFFECTUE",
                "montant", 20
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 si statut est absent.
     * @Return: HTTP 400 (statut obligatoire).
     */
    @Test
    void savePayment_MissingStatus() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "WALLET",
                "montant", 20,
                "walletProvider", "PAYPAL"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 si montant est absent.
     * @Return: HTTP 400 (montant obligatoire).
     */
    @Test
    void savePayment_MissingMontant() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "WALLET",
                "statut", "EFFECTUE",
                "walletProvider", "PAYPAL"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 si montant est negatif.
     * @Return: HTTP 400 (regle @Min).
     */
    @Test
    void savePayment_NegativeMontant() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "WALLET",
                "statut", "EFFECTUE",
                "montant", -1,
                "walletProvider", "PAYPAL"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 pour un paiement CARTE sans CVV.
     * @Return: HTTP 400 (erreur de validation metier).
     */
    @Test
    void savePayment_CarteMissingCvv() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "CARTE",
                "statut", "EFFECTUE",
                "montant", 20,
                "cardOwner", "Jean Dupont",
                "cardNumber", "4970100000000000",
                "cardExpiryDate", "12/30"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 pour un paiement WALLET avec walletProvider invalide.
     * @Return: HTTP 400 (enum walletProvider invalide).
     */
    @Test
    void savePayment_WalletInvalidProvider() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "WALLET",
                "statut", "EFFECTUE",
                "montant", 20,
                "walletProvider", "STRIPE"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 pour un paiement CARTE sans cardOwner.
     * @Return: HTTP 400 (cardOwner obligatoire pour CARTE).
     */
    @Test
    void savePayment_CarteMissingOwner() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "CARTE",
                "statut", "EFFECTUE",
                "montant", 20,
                "cardNumber", "4970100000000000",
                "cardExpiryDate", "12/30",
                "cvv", "123"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 pour un paiement CARTE sans cardNumber.
     * @Return: HTTP 400 (cardNumber obligatoire pour CARTE).
     */
    @Test
    void savePayment_CarteMissingNumber() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "CARTE",
                "statut", "EFFECTUE",
                "montant", 20,
                "cardOwner", "Jean Dupont",
                "cardExpiryDate", "12/30",
                "cvv", "123"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 pour un paiement CARTE avec cardNumber invalide.
     * @Return: HTTP 400 (format cardNumber invalide).
     */
    @Test
    void savePayment_CarteInvalid() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "CARTE",
                "statut", "EFFECTUE",
                "montant", 20,
                "cardOwner", "Jean Dupont",
                "cardNumber", "497010000000000",
                "cardExpiryDate", "12/30",
                "cvv", "123"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 pour un paiement CARTE sans cardExpiryDate.
     * @Return: HTTP 400 (cardExpiryDate obligatoire pour CARTE).
     */
    @Test
    void savePayment_CarteMissingExpiryDate() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "CARTE",
                "statut", "EFFECTUE",
                "montant", 20,
                "cardOwner", "Jean Dupont",
                "cardNumber", "4970100000000000",
                "cvv", "123"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 pour un paiement CARTE avec cardExpiryDate invalide.
     * @Return: HTTP 400 (format MM/YY invalide).
     */
    @Test
    void savePayment_CarteInvalidExpiryDate() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "CARTE",
                "statut", "EFFECTUE",
                "montant", 20,
                "cardOwner", "Jean Dupont",
                "cardNumber", "4970100000000000",
                "cardExpiryDate", "13/30",
                "cvv", "123"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 pour un paiement CARTE avec CVV invalide.
     * @Return: HTTP 400 (format CVV invalide).
     */
    @Test
    void savePayment_CarteInvalidCvv() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "CARTE",
                "statut", "EFFECTUE",
                "montant", 20,
                "cardOwner", "Jean Dupont",
                "cardNumber", "4970100000000000",
                "cardExpiryDate", "12/30",
                "cvv", "12"
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 pour un paiement WALLET sans walletProvider.
     * @Return: HTTP 400 (erreur de validation metier).
     */
    @Test
    void savePayment_WalletMissingProvider() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "WALLET",
                "statut", "EFFECTUE",
                "montant", 20
        );

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne 400 si walletProvider est fourni pour un paiement CARTE.
     * @Return: HTTP 400 (donnees incoherentes avec le type de paiement).
     */
    @Test
    void savePayment_CardWithWallet() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "CARTE",
                "statut", "EFFECTUE",
                "montant", 20,
                "walletProvider", "PAYPAL",
                "cardOwner", "Jean Dupont",
                "cardNumber", "4970100000000000",
                "cardExpiryDate", "12/30",
                "cvv", "123"
        );

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete POST retourne une erreur 400 si une valeur enum est invalide.
     * @Return: HTTP 400 (erreur de deserialisation/validation enum).
     */
    @Test
    void savePayment_EnumInvalid() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "type", "BITCOIN",
                "statut", "EFFECTUE",
                "montant", 20
        );

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    /**
     * @Description: verifie que la requete DELETE supprime un paiement existant.
     * @Return: HTTP 200 + booleen true.
     */
    @Test
    void deletePayment() throws Exception {
        when(paymentService.deletePayment(3L)).thenReturn(true);

        mockMvc.perform(delete("/payments/3")).andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * @Description: verifie que la requete DELETE retourne une erreur 404 si l'id n'existe pas.
     * @Return: HTTP 404 + message d'erreur.
     */
    @Test
    void deletePayment_NotFound() throws Exception {
        when(paymentService.deletePayment(anyLong()))
                .thenThrow(new EntityNotFoundException("Le paiement avec l'ID 66 n'existe pas"));

        mockMvc.perform(delete("/payments/66")).andExpect(status().isNotFound())
                .andExpect(content().string("Le paiement avec l'ID 66 n'existe pas"));
    }
}
