package pt.tecnico.bftb.server.domain;

public interface Label {

    String UNKNOWN_ERROR = "An unexpected error occurred.";
    String INVALID_PUBLIC_KEY = "Public key given is invalid.";
    String SUCC_ACC_CRT = "Account created with success.";
    String ERR_ACC_CRT = "User already has an account.";
    String ERR_NO_ACC = "There is no account registered with given public key.";
    String WAIT_ACC = "Your request was processed and is waiting for acceptance from the receiver.";
    String INVALID_AMOUNT = "You can only transfer positive quantities.";
    String NO_PENDING_TRANSACTIONS = "Account doesn't have any incoming pending transactions.";
    String INVALID_ARGS_SEND_AMOUNT = "Source and destination key must be different.";
    String INVALID_TRANSACTION_ID = "Transaction id is an integer greater or equal to 1. To see the pending incoming" +
            " transfers id's type \"check_account yourPublicKey\".";
    String SUCCESS_TRANSACTION = "Transaction occurred successfully.";
    String SUCCESS_TRANSACTION_REJECTED = "Transaction rejected successfully.";
    String NON_EXISTENT_TRANSACTION = "There is no transaction between given public key account and this account.";
    String NOT_ENOUGH_BALANCE = "Account has no sufficient funds to execute transaction.";
    String NO_AUTHORIZATION = "This account has no authorization to accept or reject this transaction.";
    String SUCCESS = "SUCCESS";
    String ERROR_TRANSACTION_TIMEOUT = "Database timeout expired for transaction.";
    String ERROR_TRANSACTION = "An unexpected error occurred in the database.";
}
