package it.unipi.myfuture.myfuture_backend.dto.user;

import it.unipi.myfuture.myfuture_backend.model.RecentTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO to return the last transaction related to the user.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTransactionsResponseDTO {

    private List<RecentTransaction> transactions;   // the last transaction related to the user (10 at most)
    private int count;                              // The number of transactions included in this DTO
}
