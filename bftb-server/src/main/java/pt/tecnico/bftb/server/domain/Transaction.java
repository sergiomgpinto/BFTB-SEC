package pt.tecnico.bftb.server.domain;

import java.security.PublicKey;

public class Transaction {

    private final String _publicKey;//publicKey of other account
    private final int _amount;
    private final TransactionType _type;

    public Transaction(String publicKey, int amount, TransactionType type){
        _publicKey = publicKey;
        _amount = amount;
        _type = type;
    }

    public String getPublicKey(){
        return _publicKey;
    }

    public int get_amount() {
        return _amount;
    }

    public TransactionType get_type() {
        return _type;
    }

    public String toString(){
        return "Other entity's publicKey: " + _publicKey + " | Amount: " + String.valueOf(_amount)
                + " | Type: " + _type.toString();
    }
}