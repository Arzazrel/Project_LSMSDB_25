package it.unipi.myfuture.myfuture_backend.controller;

import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for authenticated customers.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private TransactionService transactionService;

    // ------------------------------------------------ start: transaction API --------------------------------------------------

    @GetMapping("/{userId}/transactions")
    public List<TransactionResponseDTO> getUserTransactions(@PathVariable Long userId) {
        return transactionService.getTransactionsByUser(userId);
    }

    @GetMapping("/transactions/{id}")
    public TransactionResponseDTO getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id);
    }

    // ------------------------------------------------ end: transaction API --------------------------------------------------
}