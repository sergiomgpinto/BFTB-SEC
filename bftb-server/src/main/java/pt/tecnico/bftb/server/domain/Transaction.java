package pt.tecnico.bftb.server.domain;

public class Transaction {

    private final String _publicKey;// PublicKey of other account to which the account with this
    // transaction has a transaction together.
    private final int _amount;
    private final TransactionType _type;
    private final String _status;

    public Transaction(String publicKey, int amount, TransactionType type, String status){
        _publicKey = publicKey;
        _amount = amount;
        _type = type;
        _status = status;
    }

    /*************************************Getters***********************************/

    /**
     * @return public key in string format.
     */
    public String getPublicKey(){
        return _publicKey;
    }

    /**
     * @return amount of transaction.
     */
    public int getAmount() {
        return _amount;
    }

    /**
     * @return either CREDIT or WITHDRAWAL.
     */
    public TransactionType getType() {
        return _type;
    }

    /**
     * @return string format of transaction.
     */
    public String toString(){
        return "Other entity's publicKey: " + _publicKey + " | Amount: " + String.valueOf(_amount)
                + " | Type: " + _type.toString() + " | Status: " + _status.toString();
    }
}