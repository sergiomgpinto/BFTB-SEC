package pt.tecnico.bftb.server.domain;

import com.google.protobuf.ByteString;
import org.apache.commons.lang.RandomStringUtils;
import pt.tecnico.bftb.server.database.BFTBMySqlDriver;
import pt.tecnico.bftb.server.domain.exception.*;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;
import java.util.*;

public class BFTBServerLogic {

    // Driver that allows connection to database.
    private BFTBMySqlDriver mySqlDriver = new BFTBMySqlDriver();
    // Set of all accounts in the system.
    HashSet<Account> _accounts = new HashSet<>();
    // Set of all nonces in the system. Each user with its public key has only one nonce.
    HashMap<PublicKey, Integer> _nonces = new HashMap<>();
    // Set of all recent challenges for every account to verify proof of work.
    HashMap<PublicKey, String> _challenges = new HashMap<>();

    private int _number_of_accounts = 0;
    // Provider of function to generate randomly nonce.
    private final static SecureRandom randomGenerator = new SecureRandom();


    public BFTBServerLogic() {
        recoverBFTBServerState();
    }

    /*************************************Getters***********************************/

    /**
     * @return the nonce stored for the given user.
     */
    public int getUserNonce(String publicKey) {

        Account account = null;
        int nonce = 0;

        try {
            account = searchAccount(publicKey);
            PublicKey pubKey = account.getPublicKey();

            synchronized (this) {
                nonce = _nonces.get(pubKey);
            }
        }
        catch (NonExistentAccount nea) {
            //Should never happen.
        }

        return nonce;
    }

    /**
     * @return all public keys in string format.
     */
    public synchronized List<String> getAllPublicKeys() {
        List<String> result = new ArrayList<>();


        for (Account account : _accounts) {
            result.add(account.getPublicKeyString());
        }
        return result;
    }


    /**
     * @return a specific account with given string key.
     */
    public synchronized Account searchAccount(String key) throws NonExistentAccount {
        for (Account account : _accounts) {
            if (account.getPublicKeyString().equals(key)) {// Account already exists.
                return account;
            }
        }
        throw new NonExistentAccount(Label.ERR_NO_ACC);
    }


    /**
     * @return a specific account with given public key.
     */
    public synchronized Account searchAccount(PublicKey key) throws NonExistentAccount {
        for (Account account : _accounts) {
            if (account.getPublicKey().equals(key)) {// Account already exists.
                return account;
            }
        }
        throw new NonExistentAccount(Label.ERR_NO_ACC);
    }

    /**
     * @return respective recent proof of work challenge for given account public key.
     */
    public synchronized String getChallenge(PublicKey publicKey) {
        return _challenges.get(publicKey);
    }

    /**
     * @return respective recent proof of work challenge for given account public key string.
     */
    public synchronized String getChallenge(String publicKeyString) throws NonExistentAccount {
        Account account = searchAccount(publicKeyString);
        PublicKey publicKey = account.getPublicKey();
        return _challenges.get(publicKey);
    }
    /*************************************Setters***********************************/



    /***********************READ OPERATIONS****************/


    /**
     * @return all concluded transactions from given string format key user.
     */
    public List<String> audit(String key)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NonExistentAccount{

        List<String> set = new ArrayList<>();
        boolean ACCOUNT_FOUND = false;
      
        synchronized (this) {
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
        }

        return set;
    }

    /**
     * @return pending transactions for given string format key user.
     */
    public List<String> checkAccount(String key)
            throws NonExistentAccount {

        List<String> ret = new ArrayList<>();
        boolean ACCOUNT_FOUND = false;
        Account owner_account = null;

        synchronized (this) {
            for (Account account : _accounts) {
                if (account.getPublicKeyString().equals(key)) {// Account found.
                    ACCOUNT_FOUND = true;
                    owner_account = account;
                    ret.add(String.valueOf(account.getBalance()));
                }
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
        owner_account.incrementRid();
        return ret;
    }

    /***********************WRITE OPERATIONS****************/

    /**
     * @return a random string used for proof of work.
     */
    private String generateRandomString() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * @return a new nonce for given public key user in format bytestring.
     */
    public String[] newNonce(ByteString publicKey) {
        // If a man in the middle wanted to brute force to find the nonce, it would take him
        // 2**32 server responses to generate which ultimately for our problem at hands
        // is much much more than the time needed.
        // Also, the probability of two nonces colliding is very very low since they are almost
        // perfectly random.

        int nonce = randomGenerator.nextInt();
        PublicKey pubKey = null;
        // Generate new proof of work challenge that will be a random string.
        String challenge = generateRandomString();

        try {
            pubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey.toByteArray()));
        } catch (Exception e) {
            // Should never happen.
        }
        synchronized (this) {
            _nonces.put(pubKey, nonce);
            _challenges.put(pubKey,challenge);
        }
        return new String[]{String.valueOf(nonce),challenge};
    }

    /**
     * @return a new nonce for given public key user.
     */
    public String[] newNonce(PublicKey publicKey) {
        // Generate new nonce that will be a random int.
        int nonce = randomGenerator.nextInt();
        // Generate new proof of work challenge that will be a random string.
        String challenge = generateRandomString();
        PublicKey pubKey = null;

        try {
            pubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey.getEncoded()));
        }
        catch (Exception e) {
            // Should never happen.
        }
        synchronized (this) {
            _nonces.put(pubKey, nonce);
            _challenges.put(pubKey,challenge);
        }
        return new String[]{String.valueOf(nonce),challenge};
    }


    /**
     * @return success if account was successfully created for given params otherwise the respective errors.
     */
    public String openAccount(ByteString key, String username) throws InvalidKeySpecException, NoSuchAlgorithmException, BFTBDatabaseException {
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key.toByteArray()));
        Account newAccount;
        // This function is restricting one account per user.
        synchronized (this) {
            for (Account account : _accounts) {
                if (account.getPublicKey().equals(publicKey)) {// Account already exists.
                    return Label.ERR_ACC_CRT;
                }
            }

            _number_of_accounts += 1;
            newAccount = new Account(publicKey, username);
            _accounts.add(newAccount);

        }
        String publicKeyString = newAccount.getPublicKeyString();

        String[] args = { String.valueOf(newAccount.getBalance()), publicKeyString, username};
        // Registers information in the database.
        String ret = mySqlDriver.dbParser("openAccount",args,publicKey,null);

        if (ret.equals(Label.SUCCESS)) {
            return Label.SUCC_ACC_CRT + ":" + publicKeyString;
        } else {
            throw new BFTBDatabaseException(ret);
        }
    }

    /**
     * @return that transaction is awaiting approval for given params otherwise the respective errors.
     */
    public String sendAmount(String senderKey, String receiverKey, int amount, ByteString digitalSignature)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoAccountException, BFTBDatabaseException{

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
                String.valueOf(transactionId)};

        String ret = mySqlDriver.dbParser("sendAmount",args,null, digitalSignature);

        if (ret.equals(Label.SUCCESS)) {
            return Label.WAIT_ACC;
        } else {
            throw new BFTBDatabaseException(ret);
        }
    }

    /**
     * @return either transaction accepted or rejected successfully for given params otherwise respective errors.
     */
    public String receiveAmount(String receiverKey, String senderKey, int transactionId, boolean answer,
                                             ByteString digitalSignature)
            throws NonExistentAccount, NonExistentTransaction, NoAuthorization, BFTBDatabaseException{

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
            senderAccount.addTransaction(receiverKey, amount, TransactionType.WITHDRAWAL, "REJECTED");
            receiverAccount.addTransaction(senderKey, amount, TransactionType.CREDIT, "REJECTED");

            String answerString = "false";
            String[] args = { answerString, String.valueOf(initialAmount + amount), String.valueOf(initialAmount),
                    senderAccount.getPublicKeyString(),
                    receiverAccount.getPublicKeyString(), senderAccount.getPublicKeyString(),
                    String.valueOf(pendingTransaction.getTransactionId()),
                    String.valueOf(pendingTransaction.getAmount()), TransactionType.CREDIT.toString()};

            // Writes information to the database.
            String ret = mySqlDriver.dbParser("receiveAmount",args,null,digitalSignature);

            if (ret.equals(Label.SUCCESS)) {
                return Label.SUCCESS_TRANSACTION_REJECTED;
            } else {
                throw new BFTBDatabaseException(ret);
            }

        } else { // User accepts transaction.
            TransactionType type = pendingTransaction.getType();
            senderAccount.removePendingTransaction(receiverKey, transactionId);
            senderAccount.addTransaction(receiverKey, amount, type, "ACCEPTED");

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

            receiverAccount.addTransaction(senderKey, amount, type, "ACCEPTED");

            String answerString = "true";
            String[] args = { answerString, String.valueOf(receiverAccount.getBalance()), String.valueOf(initialAmount),
                    receiverAccount.getPublicKeyString(),
                    receiverAccount.getPublicKeyString(), senderAccount.getPublicKeyString(),
                    String.valueOf(pendingTransaction.getTransactionId()),
                    String.valueOf(pendingTransaction.getAmount()), TransactionType.CREDIT.toString() };
            // Writes information to the database.
            String ret = mySqlDriver.dbParser("receiveAmount",args,null, digitalSignature);

            if (ret.equals(Label.SUCCESS)) {
                return Label.SUCCESS_TRANSACTION;
            } else {
                throw new BFTBDatabaseException(ret);
            }
        }
    }

    /**
     * Recovers server state from database through the mySqlDriver.
     */
    private void recoverBFTBServerState() {
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
                String username = set.getString("Username");
                try {
                    PublicKey publicKey = KeyFactory.getInstance("RSA")
                            .generatePublic(new X509EncodedKeySpec(blobData.getBytes(1, blobLength)));
                    Account account = new Account(balance, publicKeyString, publicKey, username);
                    synchronized (this) {
                        _accounts.add(account);
                    }
                }
                catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                    System.out.println(e.getMessage());
                }

            }
            synchronized (this) {
                _number_of_accounts = _accounts.size();
            }

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
                String destinationKey = set.getString("DestinationUserKey");
                String status = set.getString("Status");
                Account senderAccount = searchAccount(sourceKey);
                Account receiverAccount = searchAccount(destinationKey);

                senderAccount.addTransactionRecoverState(destinationKey,amount,TransactionType.WITHDRAWAL,status);
                receiverAccount.addTransactionRecoverState(sourceKey,amount,TransactionType.CREDIT,status);

            }
        } catch (ClassNotFoundException | SQLException cnfe) {
            // Should never happen.
        } catch (NonExistentAccount nea) {
            // Should never happen
        }
    }

    public int getAccWTS(String key) throws NonExistentAccount {
        return searchAccount(key).getWts();
    }

    public int getAccRid(String key) throws NonExistentAccount {
        return searchAccount(key).getRid();
    }
}
