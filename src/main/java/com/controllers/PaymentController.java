package com.controllers;

import com.dtos.PaymentDto;
import com.services.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentDto> getPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public PaymentDto getPayment(@PathVariable Long id) {
        return paymentService.getPayment(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDto savePayment(@Valid @RequestBody PaymentDto paymentDto) {
        return paymentService.savePayment(paymentDto);
    }

    @DeleteMapping("/{id}")
    public Boolean deletePayment(@PathVariable Long id) {
        return paymentService.deletePayment(id);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleEntityNotFound(EntityNotFoundException exception) {
        return exception.getMessage();
    }
}
