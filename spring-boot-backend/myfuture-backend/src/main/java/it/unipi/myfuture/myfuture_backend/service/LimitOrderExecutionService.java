package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.model.Transaction;

/**
 * Limit Order Service interface.
 * Handles the execution of the pending order (purchase or sell created when the market was close)
 */
public interface LimitOrderExecutionService {
    /**
     * Method for processing a pending asset purchase or sell request.
     *
     * @param request The DTO containing pending trade details
     * @return a TransactionResponseDTO representing the outcome of the processed transaction.
     * @throws BusinessException if the symbol is invalid, user is deleted, limit price isn't respected
     */
    void processPendingTrade(Transaction request);
}
