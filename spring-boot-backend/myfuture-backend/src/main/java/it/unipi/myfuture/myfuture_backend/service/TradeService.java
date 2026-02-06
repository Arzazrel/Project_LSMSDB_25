package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;

/**
 * Trade Service interface.
 * Handles financial and asset trading operations ensuring consistency between MongoDB and Redis cache.
 */
public interface TradeService {

    /**
     * Main entry point for processing an asset purchase or sell request.
     * This method checks if the New York Stock Exchange (NYSE) or NASDAQ (same operating hours) is currently open.
     * - If the market is open, it routes the request to an immediate market order execution.
     * - If the market is closed, it routes the request to a limit order placement (Pending).
     *
     * @param email email fo the user taken by the authentication context
     * @param userId  the unique identifier of the user performing the trade.
     * @param request the DTO containing trade details such as symbol, quantity, and currency.
     * @return a TransactionResponseDTO representing the outcome of the trade (SUCCESS or PENDING).
     * @throws BusinessException if the symbol is invalid, funds are insufficient, or price data is missing.
     */
    TransactionResponseDTO processTrade(String email, Long userId, TransactionRequestDTO request);
}