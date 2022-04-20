package pt.tecnico.bftb.server.domain;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Account {

    private int _balance;
    private PublicKey _publicKey = null;
    private final String _publicKeyString;
    static final String NOT_ENOUGH_BALANCE = "Account has no sufficient funds to execute transaction.";
    static final String SUCCESS_TRANSACTION = "Transaction executed with success";
    private final String _username;
    ArrayList<Transaction> _transactions = new ArrayList<>();
    ArrayList<Pending> _pending = new ArrayList<>();

    //Constructor used when server is running normally.
    public Account(PublicKey publicKey, String username) {
        _balance = 1000;
        _publicKey = publicKey;
        _publicKeyString = "PublicKey" + username.replaceAll("\\D+","");
        _username = username;

    }

    //Constructor used when recovering state from database.
    public Account(int balance, String publicKeyString, PublicKey publicKey, String username) {
        _balance = balance;
        _publicKeyString = publicKeyString;
        _publicKey = publicKey;
        _username = username;
    }

    public int getBalance() {
        return _balance;
    }

    public PublicKey getPublicKey() {
        return _publicKey;
    }

    public String getPublicKeyString() {
        return _publicKeyString;
    }

    public boolean subtractBalance(int amount) {
        if (_balance - amount > 0) {
            _balance -= amount;
            return true;
        } else {
            return false;
        }
    }

    public void addBalance(int amount) {
        _balance += amount;
    }

    public synchronized Pending getPendingTransaction(String publicKey, int transactionId) {
        for (Pending pending : _pending) {
            if (pending.getTransactionId() == transactionId && publicKey.equals(pending.getSenderKey())) {
                return pending;
            }
        }
        // Should never happen.
        return null;
    }

    public synchronized void removePendingTransaction(String senderKey, int transactionId) {

        _pending.removeIf(
                pending -> pending.getTransactionId() == transactionId && senderKey.equals(pending.getSenderKey()));
    }

    public synchronized String addTransaction(String publicKey, int amount, TransactionType type) {

        if (type == TransactionType.CREDIT) {
            _balance += amount;

            Transaction transaction = new Transaction(publicKey, amount, type);
            _transactions.add(transaction);
            return SUCCESS_TRANSACTION;
        } else {
            // We don't subtract balance here since it was done in send_amount before.
            Transaction transaction = new Transaction(publicKey, amount, type);
            _transactions.add(transaction);

            return SUCCESS_TRANSACTION;
        }
    }

    public synchronized void addTransactionRecoverState(String publicKey, int amount, TransactionType type) {
        Transaction transaction = new Transaction(publicKey, amount, type);
        _transactions.add(transaction);
    }

    public List<Transaction> getTransactions() {
        return _transactions;
    }

    public synchronized void addPending(String publicKey, int amount, boolean isSender, int transactionId) {
        if (isSender) {
            _pending.add(new Pending(publicKey, amount, TransactionStatus.PENDING, TransactionType.WITHDRAWAL,
                    transactionId));

        } else {
            _pending.add(
                    new Pending(publicKey, amount, TransactionStatus.PENDING, TransactionType.CREDIT, transactionId));

        }
    }

    public synchronized List<Pending> getPending() {
        return _pending;
    }

    public synchronized List<Pending> getIncomingPending() {

        List<Pending> filtered = new ArrayList<>();

        for (Pending pending : _pending) {
            if (pending.getType().equals(TransactionType.CREDIT)) {
                filtered.add(pending);
            }
        }
        return filtered;
    }

}
