package it.unipi.myfuture.myfuture_backend.dto.user;

import it.unipi.myfuture.myfuture_backend.enums.UserCurrency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to return the cash fields related to the user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCashResponseDTO {
    private double cash;                // cash available (liquidity)
    private double blockedCash;         // cash blocked for transaction (purchase)
    private UserCurrency currency;      // currency, default USD (for now only USD)
}
