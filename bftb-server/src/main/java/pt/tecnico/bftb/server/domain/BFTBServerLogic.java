package pt.tecnico.bftb.server.domain;

import com.google.protobuf.ByteString;
import pt.tecnico.bftb.server.domain.exception.*;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class BFTBServerLogic {

    HashSet<Account> _accounts = new HashSet<>();
    HashMap<PublicKey, Integer> nonces = new HashMap<>();
    private int _number_of_accounts = 0;
    private final static SecureRandom randomGenerator = new SecureRandom();

    public synchronized int newNonce(ByteString publicKey) {
        // If a man in the middle wanted to brute force to find the nonce, it would take
        // him
        // 2**32 server responses to generate which ultimately for our problem at hands
        // is much much more than the time needed.
        // Also, the probability of two nonces colliding is very very low since they are
        // almost
        // perfectly random.

        int nonce = randomGenerator.nextInt();
        PublicKey pubKey = null;

        try {
            pubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey.toByteArray()));
        } catch (Exception e) {
            // Should never happen.
        }
        nonces.put(pubKey, nonce);
        return nonce;
    }

    public synchronized int getUserNonce(String publicKey) {

        Account account = null;
        int nonce = 0;

        try {
            account = searchAccount(publicKey);
            PublicKey pubKey = account.getPublicKey();
            nonce = nonces.get(pubKey);
        } catch (NonExistentAccount nea) {
            // Should never happen.
        }

        return nonce;
    }

    public synchronized int newNonce(PublicKey publicKey) {
        int nonce = randomGenerator.nextInt();
        PublicKey pubKey = null;

        try {
            pubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey.getEncoded()));
        } catch (Exception e) {
            // Should never happen.
        }
        nonces.put(pubKey, nonce);
        return nonce;
    }

    public synchronized String openAccount(ByteString key, String username)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key.toByteArray()));

        // This function is restricting one account per user.
        for (Account account : _accounts) {
            if (account.getPublicKey().equals(publicKey)) {// Account already exists.
                return Label.ERR_ACC_CRT;
            }
        }

        _number_of_accounts += 1;
        Account account = new Account(publicKey, _number_of_accounts, username);
        _accounts.add(account);
        String publicKeyString = account.getPublicKeyString();

        String[] args = { String.valueOf(account.getBalance()), publicKeyString };

        return Label.SUCC_ACC_CRT + ":" + publicKeyString;
    }

    public synchronized String sendAmount(String senderKey, String receiverKey, int amount, int wts)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoAccountException {

        Account senderAccount;
        try {
            senderAccount = searchAccount(senderKey);
        } catch (NonExistentAccount e) {
            throw new NoAccountException("There are no accounts associated to this Public Key.");
        }
        Account receiverAccount;
        try {
            receiverAccount = searchAccount(receiverKey);
        } catch (NonExistentAccount e) {
            throw new NoAccountException("The account to which you are trying to send money does not exist.");
        }

        int initialBalance = senderAccount.getBalance();
        boolean doesAccHaveSuffFunds = senderAccount.subtractBalance(amount);

        if (!doesAccHaveSuffFunds) {
            return Label.NOT_ENOUGH_BALANCE;
        }
        int transactionId = receiverAccount.getIncomingPending().size() + 1;
        receiverAccount.addPending(senderKey, amount, false, transactionId);
        senderAccount.addPending(receiverKey, amount, true, transactionId);
        senderAccount.setWts(wts);

        return Label.WAIT_ACC;
    }

    public synchronized List<String> audit(String key)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NonExistentAccount {

        List<String> set = new ArrayList<>();
        boolean ACCOUNT_FOUND = false;

        for (Account account : _accounts) {
            String publicKey = account.getPublicKeyString();
            if (key.equals(publicKey)) {
                // Account found.
                ACCOUNT_FOUND = true;
                List<Transaction> transactions = account.getTransactions();

                for (Transaction transaction : transactions) {
                    set.add(transaction.toString());
                }
            }
        }
        if (!ACCOUNT_FOUND) {
            throw new NonExistentAccount(Label.ERR_NO_ACC);
        }
        return set;
    }

    public synchronized List<String> checkAccount(String key)
            throws NonExistentAccount {

        List<String> ret = new ArrayList<>();
        boolean ACCOUNT_FOUND = false;
        Account owner_account = null;

        for (Account account : _accounts) {
            if (account.getPublicKeyString().equals(key)) {// Account found.
                ACCOUNT_FOUND = true;
                owner_account = account;
                ret.add(String.valueOf(account.getBalance()));
            }
        }
        // Account not found.
        if (!ACCOUNT_FOUND) {
            throw new NonExistentAccount(Label.ERR_NO_ACC);
        }
        // Returns list of pending incoming transfers
        for (Pending pendingTransaction : owner_account.getPending()) {
            if (pendingTransaction.getType() == TransactionType.CREDIT) {
                ret.add(pendingTransaction.toString(owner_account.getPublicKeyString()));
            }
        }
        return ret;
    }

    public synchronized List<String> getAllPublicKeys() {
        List<String> result = new ArrayList<>();

        for (Account account : _accounts) {
            result.add(account.getPublicKeyString());
        }
        return result;
    }

    public synchronized String receiveAmount(String receiverKey, String senderKey, int transactionId, boolean answer,
            int wts)
            throws NonExistentAccount, NonExistentTransaction, NoAuthorization {

        Account receiverAccount = null;
        Account senderAccount = null;
        try {
            receiverAccount = searchAccount(receiverKey);
            senderAccount = searchAccount(senderKey);
        } catch (NonExistentAccount e1) {
            throw new NonExistentAccount(Label.ERR_NO_ACC);
        }

        Pending pendingTransaction = senderAccount.getPendingTransaction(receiverKey, transactionId);

        if (pendingTransaction == null) {
            throw new NonExistentTransaction(Label.NON_EXISTENT_TRANSACTION);
        }

        if (pendingTransaction.getType().equals(TransactionType.CREDIT)) {

            String transactionSenderKey = pendingTransaction.getSenderKey();
            String transactionReceiverKey = senderAccount.getPublicKeyString();

            if (!senderKey.equals(transactionSenderKey) || !receiverKey.equals(transactionReceiverKey)) {
                throw new NoAuthorization(Label.NO_AUTHORIZATION);
            }
        }

        int amount = pendingTransaction.getAmount();// Amount is the same for both pending transactions registered
        // for sender and receiver.

        if (!answer) { // User rejects transaction.
            senderAccount.removePendingTransaction(receiverKey, transactionId);
            int initialAmount = senderAccount.getBalance();
            senderAccount.addBalance(amount);

            pendingTransaction = receiverAccount.getPendingTransaction(senderKey, transactionId);
            if (pendingTransaction == null) {
                throw new NonExistentTransaction(Label.NON_EXISTENT_TRANSACTION);
            }

            if (pendingTransaction.getType().equals(TransactionType.CREDIT)) {
                String transactionSenderKey = pendingTransaction.getSenderKey();
                String transactionReceiverKey = receiverAccount.getPublicKeyString();

                if (!senderKey.equals(transactionSenderKey) || !receiverKey.equals(transactionReceiverKey)) {
                    throw new NoAuthorization(Label.NO_AUTHORIZATION);
                }
            }

            receiverAccount.removePendingTransaction(senderKey, transactionId);

            return Label.SUCCESS_TRANSACTION_REJECTED;

        } else { // User accepts transaction.
            TransactionType type = pendingTransaction.getType();
            senderAccount.removePendingTransaction(receiverKey, transactionId);
            senderAccount.addTransaction(receiverKey, amount, type);

            pendingTransaction = receiverAccount.getPendingTransaction(senderKey, transactionId);
            if (pendingTransaction == null) {
                throw new NonExistentTransaction(Label.NON_EXISTENT_TRANSACTION);
            }

            if (pendingTransaction.getType().equals(TransactionType.CREDIT)) {
                String transactionSenderKey = pendingTransaction.getSenderKey();
                String transactionReceiverKey = receiverAccount.getPublicKeyString();

                if (!senderKey.equals(transactionSenderKey) || !receiverKey.equals(transactionReceiverKey)) {
                    throw new NoAuthorization(Label.NO_AUTHORIZATION);
                }
            }

            amount = pendingTransaction.getAmount();
            type = pendingTransaction.getType();
            receiverAccount.removePendingTransaction(senderKey, transactionId);
            int initialAmount = receiverAccount.getBalance();
            receiverAccount.addTransaction(senderKey, amount, type);
            receiverAccount.setWts(wts);
            return Label.SUCCESS_TRANSACTION;
        }
    }

    public synchronized Account searchAccount(String key) throws NonExistentAccount {
        for (Account account : _accounts) {
            if (account.getPublicKeyString().equals(key)) {// Account already exists.
                return account;
            }
        }
        throw new NonExistentAccount(Label.ERR_NO_ACC);
    }

    public synchronized Account searchAccount(PublicKey key) throws NonExistentAccount {
        for (Account account : _accounts) {
            if (account.getPublicKey().equals(key)) {// Account already exists.
                return account;
            }
        }
        throw new NonExistentAccount(Label.ERR_NO_ACC);
    }

    public int getAccWTS(String key) throws NonExistentAccount {
        return searchAccount(key).getWts();
    }
}
