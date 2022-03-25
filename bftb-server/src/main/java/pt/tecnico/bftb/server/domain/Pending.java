package pt.tecnico.bftb.server.domain;

import java.security.PublicKey;

public class Pending {
    String _senderKey;
    int _amount;
    int _transactionId;
    TransactionStatus _status;
    TransactionType _type;

    public Pending(String senderKey, int amount, TransactionStatus status, TransactionType type, int transactionId) {
        _senderKey = senderKey;
        _amount = amount;
        _status = status;
        _type = type;
        _transactionId = transactionId;
    }

    public void setSenderKey(String senderKey) {
        _senderKey = senderKey;
    }

    public String getSenderKey() {
        return _senderKey;
    }

    public void setAmount(int amount) {
        _amount = amount;
    }

    public int getAmount() {
        return _amount;
    }

    public void setComplete() {
        _status = TransactionStatus.COMPLETE;
    }

    public TransactionStatus getStatus() {
        return _status;
    }

    public TransactionType getType() {
        return _type;
    }

    public int getTransactionId(){
        return _transactionId;
    }

    public String toString(String dstPublicKey) {
        return "TransactionId: " + String.valueOf(_transactionId) + " -> Owner of the account with "
                + _senderKey + " wants to transfer " + String.valueOf(_amount)
                + " currency units to account with " + dstPublicKey + ".";
    }
}
