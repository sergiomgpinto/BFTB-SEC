package pt.tecnico.bftb.server.domain;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class Account {

    private int _balance;
    private PublicKey _publicKey;
    private final String _publicKeyString;
    private final String _username;

    // List of all concluded transactions of this account.
    private ArrayList<Transaction> _transactions = new ArrayList<>();
    // List of all pending transactions of this account.
    private ArrayList<Pending> _pending = new ArrayList<>();

    // Constructor used when server is running normally.
    public Account(PublicKey publicKey, String username) {
        _balance = 1000;
        _publicKey = publicKey;
        _publicKeyString = "PublicKey" + username.replaceAll("\\D+","");
        _username = username;

    }

    // Constructor used when recovering state from database.
    public Account(int balance, String publicKeyString, PublicKey publicKey, String username) {
        _balance = balance;
        _publicKeyString = publicKeyString;
        _publicKey = publicKey;
        _username = username;
    }

    /*************************************Getters***********************************/

    /**
     * @return the balance
     */
    public synchronized int getBalance() {
        return _balance;
    }

    /**
     * @return the public key
     */
    public PublicKey getPublicKey() {
        return _publicKey;
    }

    /**
     * @return the public key in format string
     */
    public String getPublicKeyString() {
        return _publicKeyString;
    }

    /**
     * @return the pending transaction with given publicKey and transactionId
     */
    public synchronized Pending getPendingTransaction(String publicKey, int transactionId) {
        for (Pending pending : _pending) {
            if (pending.getTransactionId() == transactionId && publicKey.equals(pending.getSenderKey())) {
                return pending;
            }
        }
        // Should never happen.
        return null;
    }

    /**
     * @return all pending transactions.
     */
    public synchronized List<Pending> getPending() {
        return _pending;
    }

    /**
     * @return all pending transactions from receiving users.
     */
    public synchronized List<Pending> getIncomingPending() {

        List<Pending> filtered = new ArrayList<>();

        for (Pending pending : _pending) {
            if (pending.getType().equals(TransactionType.CREDIT)) {
                filtered.add(pending);
            }
        }
        return filtered;
    }

    /*************************************Setters***********************************/

    /**
     * Adds amount to account balance.
     */
    public synchronized void addBalance(int amount) {
        _balance += amount;
    }

    /**
     * @return true if there are sufficient funds otherwise false.
     */
    public synchronized boolean subtractBalance(int amount) {
        if (_balance - amount > 0) {
            _balance -= amount;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes pending transaction with given senderKey and transactionId.
     */
    public synchronized void removePendingTransaction(String senderKey, int transactionId) {
        _pending.removeIf(
                pending -> pending.getTransactionId() == transactionId && senderKey.equals(pending.getSenderKey()));
    }

    /**
     * Adds pending transaction with given params.
     */
    public synchronized void addPending(String publicKey, int amount, boolean isSender, int transactionId) {
        if (isSender) {
            _pending.add(new Pending(publicKey, amount, TransactionStatus.PENDING, TransactionType.WITHDRAWAL,
                    transactionId));

        } else {
            _pending.add(
                    new Pending(publicKey, amount, TransactionStatus.PENDING, TransactionType.CREDIT, transactionId));

        }
    }

    /**
     * @return adds transaction with given params returning success.
     */
    public synchronized String addTransaction(String publicKey, int amount, TransactionType type) {

        if (type == TransactionType.CREDIT) {
            _balance += amount;

            Transaction transaction = new Transaction(publicKey, amount, type);
            _transactions.add(transaction);
        } else {
            // We don't subtract balance here since it was done in send_amount before.
            Transaction transaction = new Transaction(publicKey, amount, type);
            _transactions.add(transaction);
        }
        return Label.SUCCESS_TRANSACTION;
    }

    /**
     * @return adds transaction with given params returning success. Used when recovering server
     * data from database.
     */
    public synchronized void addTransactionRecoverState(String publicKey, int amount, TransactionType type) {
        Transaction transaction = new Transaction(publicKey, amount, type);
        _transactions.add(transaction);
    }

    /**
     * @return all transactions.
     */
    public synchronized List<Transaction> getTransactions() {
        return _transactions;
    }

}
