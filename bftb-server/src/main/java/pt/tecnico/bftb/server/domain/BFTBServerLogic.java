package pt.tecnico.bftb.server.domain;

import com.google.protobuf.ByteString;
import pt.tecnico.bftb.server.domain.exception.NoAuthorization;
import pt.tecnico.bftb.server.domain.exception.NonExistentAccount;
import pt.tecnico.bftb.server.domain.exception.NonExistentTransaction;

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
        int nonce = randomGenerator.nextInt();
        PublicKey pubKey = null;

        try {
            pubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey.toByteArray()));
        }
        catch (Exception e) {
            // Should never happen.
        }
        nonces.put(pubKey, nonce);
        System.out.println(pubKey);
        return nonce;
    }

    public synchronized String openAccount(ByteString key) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key.toByteArray()));

        // This function is restricting one account per user.
        for (Account account : _accounts) {
            if (account.getPublicKey().equals(publicKey)) {// Account already exists.
                return Label.ERR_ACC_CRT;
            }
        }
        _number_of_accounts += 1;
        Account account = new Account(publicKey, _number_of_accounts);
        _accounts.add(account);
        String publicKeyString = account.getPublicKeyString();
        return Label.SUCC_ACC_CRT + ":" + publicKeyString;
    }

    public synchronized String sendAmount(String senderKey, String receiverKey, int amount)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoAccountException {

        Account senderAccount = searchAccount(senderKey);
        Account receiverAccount = searchAccount(receiverKey);

        if (senderAccount == null) {
            throw new NoAccountException("There are no accounts associated to this Public Key.");

        } else if (receiverAccount == null) {
            throw new NoAccountException("The account to which you are trying to send money does not exist.");
        }
        boolean doesAccHaveSuffFunds = senderAccount.subtractBalance(amount);

        if (!doesAccHaveSuffFunds){
            return Label.NOT_ENOUGH_BALANCE;
        }
        int transactionId = receiverAccount.getIncomingPending().size() + 1;
        receiverAccount.addPending(senderKey, amount, false, transactionId);
        senderAccount.addPending(receiverKey, amount, true,transactionId);

        return Label.WAIT_ACC;
    }

    public List<String> audit(String key) throws InvalidKeySpecException, NoSuchAlgorithmException, NonExistentAccount {

        List<String> set = new ArrayList<>();
        boolean ACCOUNT_FOUND = false;

        for (Account account : _accounts) {
            String publicKey = account.getPublicKeyString();
            if (key.equals(publicKey)) {
                //Account found.
                ACCOUNT_FOUND = true;
                List<Transaction> transactions = account.getTransactions();

                for (Transaction transaction : transactions) {
                    set.add(transaction.toString());
                }
            }
        }
        if (!ACCOUNT_FOUND){
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
        //Account not found.
        if (!ACCOUNT_FOUND){
            throw new NonExistentAccount(Label.ERR_NO_ACC);
        }
        //Returns list of pending incoming transfers
        for (Pending pendingTransaction: owner_account.getPending()) {
            if (pendingTransaction.getType() == TransactionType.CREDIT) {
                ret.add(pendingTransaction.toString(owner_account.getPublicKeyString()));
            }
        }
        return ret;
    }

    public List<String> getAllPublicKeys() {
        List<String> result = new ArrayList<>();

        for (Account account : _accounts) {
            result.add(account.getPublicKeyString());
        }
        return result;
    }

    public String receiveAmount(String receiverKey, String senderKey, int transactionId, boolean answer)
            throws NonExistentAccount, NonExistentTransaction, NoAuthorization {

        Account receiverAccount = searchAccount(receiverKey);
        Account senderAccount = searchAccount(senderKey);

        if (receiverAccount == null) {
            throw new NonExistentAccount(Label.ERR_NO_ACC);

        } else if (senderAccount == null) {
            throw new NonExistentAccount(Label.ERR_NO_ACC);
        }

        Pending pendingTransaction = senderAccount.getPendingTransaction(receiverKey,transactionId);

        if (pendingTransaction == null){
            throw new NonExistentTransaction(Label.NON_EXISTENT_TRANSACTION);
        }

        if (pendingTransaction.getType().equals(TransactionType.CREDIT)) {

            String transactionSenderKey = pendingTransaction.getSenderKey();
            String transactionReceiverKey = senderAccount.getPublicKeyString();

            if (!senderKey.equals(transactionSenderKey) || !receiverKey.equals(transactionReceiverKey)){
                throw new NoAuthorization(Label.NO_AUTHORIZATION);
            }
        }

        int amount = pendingTransaction.getAmount();// Amount is the same for both pending transactions registered
        //for sender and receiver.

        if (!answer) { // User accepts transaction.
            senderAccount.removePendingTransaction(receiverKey,transactionId);
            senderAccount.addBalance(amount);

            pendingTransaction = receiverAccount.getPendingTransaction(senderKey,transactionId);
            if (pendingTransaction == null){
                throw new NonExistentTransaction(Label.NON_EXISTENT_TRANSACTION);
            }

            if (pendingTransaction.getType().equals(TransactionType.CREDIT)) {
                String transactionSenderKey = pendingTransaction.getSenderKey();
                String transactionReceiverKey = receiverAccount.getPublicKeyString();

                if (!senderKey.equals(transactionSenderKey) || !receiverKey.equals(transactionReceiverKey)){
                    throw new NoAuthorization(Label.NO_AUTHORIZATION);
                }
            }

            receiverAccount.removePendingTransaction(senderKey,transactionId);

            return Label.SUCCESS_TRANSACTION_REJECTED;
        }
        else { // User rejects transaction.
            TransactionType type = pendingTransaction.getType();
            senderAccount.removePendingTransaction(receiverKey,transactionId);
            senderAccount.addTransaction(receiverKey, amount, type);

            pendingTransaction = receiverAccount.getPendingTransaction(senderKey,transactionId);
            if (pendingTransaction == null){
                throw new NonExistentTransaction(Label.NON_EXISTENT_TRANSACTION);
            }

            if (pendingTransaction.getType().equals(TransactionType.CREDIT)) {
                String transactionSenderKey = pendingTransaction.getSenderKey();
                String transactionReceiverKey = receiverAccount.getPublicKeyString();

                if (!senderKey.equals(transactionSenderKey) || !receiverKey.equals(transactionReceiverKey)){
                    throw new NoAuthorization(Label.NO_AUTHORIZATION);
                }
            }

            amount = pendingTransaction.getAmount();
            type = pendingTransaction.getType();
            receiverAccount.removePendingTransaction(senderKey,transactionId);
            receiverAccount.addTransaction(senderKey, amount, type);

            return Label.SUCCESS_TRANSACTION;
        }
    }

    public Account searchAccount(String key) {
        for (Account account : _accounts) {
            if (account.getPublicKeyString().equals(key)) {// Account already exists.
                return account;
            }
        }
        return null;

    }
}
