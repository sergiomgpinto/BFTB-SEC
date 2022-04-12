package pt.tecnico.bftb.server.domain;

import com.google.protobuf.ByteString;
import pt.tecnico.bftb.server.domain.exception.*;
import pt.tecnico.bftb.server.database.BFTBMySqlDriver;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class BFTBServerLogic {

    private BFTBMySqlDriver mySqlDriver = new BFTBMySqlDriver();
    HashSet<Account> _accounts = new HashSet<>();
    HashMap<PublicKey, Integer> nonces = new HashMap<>();
    private int _number_of_accounts = 0;
    private final static SecureRandom randomGenerator = new SecureRandom();

    public synchronized int newNonce(ByteString publicKey) {
        // If a man in the middle wanted to brute force to find the nonce, it would take him
        // 2**32 server responses to generate which ultimately for our problem at hands
        // is much much more than the time needed.
        // Also, the probability of two nonces colliding is very very low since they are almost
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
        }
        catch (NonExistentAccount nea) {
            //Should never happen.
        }

        return nonce;
    }

    public synchronized int newNonce(PublicKey publicKey) {
        int nonce = randomGenerator.nextInt();
        PublicKey pubKey = null;

        try {
            pubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey.getEncoded()));
        }
        catch (Exception e) {
            // Should never happen.
        }
        nonces.put(pubKey, nonce);
        return nonce;
    }

    public BFTBServerLogic() {
        recoverBFTBServerState();
    }

    public synchronized String openAccount(ByteString key) throws InvalidKeySpecException, NoSuchAlgorithmException
            ,BFTBDatabaseException {
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

        String[] args = { String.valueOf(account.getBalance()), publicKeyString };

        String ret = mySqlDriver.dbParser("openAccount",args,publicKey);

        if (ret.equals(Label.SUCCESS)) {
            return Label.SUCC_ACC_CRT + ":" + publicKeyString;
        } else {
            throw new BFTBDatabaseException(ret);
        }

    }

    public synchronized String sendAmount(String senderKey, String receiverKey, int amount)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoAccountException, BFTBDatabaseException {

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

        String[] args = { String.valueOf(senderAccount.getBalance()), String.valueOf(initialBalance),
                senderAccount.getPublicKeyString(), String.valueOf(amount), TransactionStatus.PENDING.toString(),
                senderAccount.getPublicKeyString(), receiverAccount.getPublicKeyString(),
                String.valueOf(transactionId) };

        String ret = mySqlDriver.dbParser("sendAmount",args,null);

        if (ret.equals(Label.SUCCESS)) {
            return Label.WAIT_ACC;
        } else {
            throw new BFTBDatabaseException(ret);
        }
    }

    public synchronized List<String> audit(String key)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NonExistentAccount, BFTBDatabaseException {

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

    public synchronized String receiveAmount(String receiverKey, String senderKey, int transactionId, boolean answer)
            throws NonExistentAccount, NonExistentTransaction, NoAuthorization, BFTBDatabaseException {

        Account receiverAccount = null;
        Account senderAccount = null;
        try{
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
            String answerString = "false";
            String[] args = { answerString, String.valueOf(initialAmount + amount), String.valueOf(initialAmount),
                    senderAccount.getPublicKeyString(),
                    receiverAccount.getPublicKeyString(), senderAccount.getPublicKeyString(),
                    String.valueOf(pendingTransaction.getTransactionId()) };

            String ret = mySqlDriver.dbParser("receiveAmount",args,null);

            if (ret.equals(Label.SUCCESS)) {
                return Label.SUCCESS_TRANSACTION_REJECTED;
            } else {
                throw new BFTBDatabaseException(ret);
            }

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

            String answerString = "true";
            String[] args = { answerString, String.valueOf(receiverAccount.getBalance()), String.valueOf(initialAmount),
                    receiverAccount.getPublicKeyString(),
                    receiverAccount.getPublicKeyString(), senderAccount.getPublicKeyString(),
                    String.valueOf(pendingTransaction.getTransactionId()),
                    String.valueOf(pendingTransaction.getAmount()), TransactionType.CREDIT.toString() };

            String ret = mySqlDriver.dbParser("receiveAmount",args,null);

            if (ret.equals(Label.SUCCESS)) {
                return Label.SUCCESS_TRANSACTION;
            } else {
                throw new BFTBDatabaseException(ret);
            }

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

    private synchronized void recoverBFTBServerState() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/bftbServer", "root", "password");

            // we trade off performance for correctness since we have very few calls to the server
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            // it saves all the changes that have been done till this particular point
            con.setAutoCommit(true);
            String sql = null;
            Statement stmt = null;

            /*Recover accounts*/

            stmt = con.createStatement();
            sql = "SELECT * FROM Account";

            ResultSet set = stmt.executeQuery(sql);

            while (set.next()) {
                int balance = set.getInt("Balance");
                String publicKeyString = set.getString("PublicKeyString");
                Blob blobData = set.getBlob("PublicKey");
                int blobLength = (int) blobData.length();

                try {
                    PublicKey publicKey = KeyFactory.getInstance("RSA")
                            .generatePublic(new X509EncodedKeySpec(blobData.getBytes(1, blobLength)));
                    Account account = new Account(balance, publicKeyString, publicKey);
                    _accounts.add(account);
                }
                catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                    System.out.println(e.getMessage());
                }

            }
            _number_of_accounts = _accounts.size();

            /*Recover pending transactions*/

            stmt = con.createStatement();
            sql = "SELECT * FROM Pending";

            set = stmt.executeQuery(sql);

            while (set.next()) {
                int amount = set.getInt("Amount");
                String sourceKey = set.getString("SourceUserKey");
                String destinationKey = set.getString("DestinationUserkey");
                int transactionId = set.getInt("TransactionId");

                Account senderAccount = searchAccount(sourceKey);
                Account receiverAccount = searchAccount(destinationKey);

                receiverAccount.addPending(sourceKey, amount, false, transactionId);
                senderAccount.addPending(destinationKey, amount, true,transactionId);
            }

            /*Recover transactions.*/

            stmt = con.createStatement();
            sql = "SELECT * FROM Transaction";

            set = stmt.executeQuery(sql);

            while (set.next()) {
                int amount = set.getInt("Amount");
                String sourceKey = set.getString("SourceUserKey");
                String destinationKey = set.getString("DestinationUserkey");

                Account senderAccount = searchAccount(sourceKey);
                Account receiverAccount = searchAccount(destinationKey);

                senderAccount.addTransactionRecoverState(destinationKey,amount,TransactionType.WITHDRAWAL);
                receiverAccount.addTransactionRecoverState(sourceKey,amount,TransactionType.CREDIT);

            }
        } catch (ClassNotFoundException | SQLException | NonExistentAccount cnfe) {
            // Should never happen.
        }
    }
}
