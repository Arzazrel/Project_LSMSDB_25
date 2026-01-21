package it.unipi.myfuture.myfuture_backend.dto.user;

import it.unipi.myfuture.myfuture_backend.model.WalletItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO to return the portfolio (wallet lists) fields related to the user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPortfolioResponseDTO {
    private List<WalletItem> shareWallet;       // list of shares held
    private List<WalletItem> etfWallet;         // list of etf held
    private List<WalletItem> cryptoWallet;      // list of crypto held
}
