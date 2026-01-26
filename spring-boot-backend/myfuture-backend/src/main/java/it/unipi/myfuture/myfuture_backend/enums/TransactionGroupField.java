package it.unipi.myfuture.myfuture_backend.enums;

/**
 * used for the input of getTransactionDistribution in TransactionAggregationDaoImpl
 */
public enum TransactionGroupField {
    transactionType,                           // indicate to group by 'type' field: sell, buy, withdrawal, deposit
    paymentMethod                   // indicate to group by 'paymentMethod' field: paypal, storeCredit, creditCard
}
