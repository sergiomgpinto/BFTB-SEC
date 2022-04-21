package pt.tecnico.bftb.server.domain;

public class Pending {
    private final String _senderKey;
    private final int _amount;
    private final int _transactionId;
    private final TransactionStatus _status;
    private final TransactionType _type;

    public Pending(String senderKey, int amount, TransactionStatus status, TransactionType type, int transactionId) {
        _senderKey = senderKey;
        _amount = amount;
        _status = status;
        _type = type;
        _transactionId = transactionId;
    }

    /*************************************Getters***********************************/

    /**
     * @return key in string format of the user that sent money.
     */
    public String getSenderKey() {
        return _senderKey;
    }

    /**
     * @return amount of transaction.
     */
    public int getAmount() {
        return _amount;
    }

    /**
     * @return either COMPLETE or PENDING.
     */
    public TransactionStatus getStatus() {
        return _status;
    }

    /**
     * @return either CREDIT or WITHDRAWAL.
     */
    public TransactionType getType() {
        return _type;
    }

    /**
     * @return id of transaction.
     */
    public int getTransactionId(){
        return _transactionId;
    }

    /**
     * @return string format of transaction.
     */
    public String toString(String dstPublicKey) {
        return "TransactionId: " + String.valueOf(_transactionId) + " -> Owner of the account with "
                + _senderKey + " wants to transfer " + String.valueOf(_amount)
                + " currency units to account with " + dstPublicKey + ".";
    }
}
