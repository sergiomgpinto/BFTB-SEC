package pt.tecnico.bftb.library;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.Provider.Service;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import pt.tecnico.bftb.grpc.Bftb.OpenAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.AuditRequest;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.EncryptedMessage;
import pt.tecnico.bftb.grpc.Bftb.EncryptedStruck;
import pt.tecnico.bftb.grpc.Bftb.NonceRequest;
import pt.tecnico.bftb.grpc.Bftb.NonceResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.Sequencemessage;
import pt.tecnico.bftb.grpc.Bftb.Unencriptedhash;

import static io.grpc.Status.UNKNOWN;

public class BFTBLibraryApp {

    PrivateKey _userPrivateKey;
    PublicKey _userPublicKey;
    PublicKey _serverPublicKey = null;

    public BFTBLibraryApp(PrivateKey privateKey, PublicKey publickey) {
        _userPrivateKey = privateKey;
        _userPublicKey = publickey;
    }

    public NonceRequest getNonce(ByteString encodedPublicKey) {
        return NonceRequest.newBuilder().setSenderKey(encodedPublicKey).build();
    }

    private byte[] hash(byte[] inputData) {

        // hash

        byte[] hash = null;
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
            sha.update(inputData);
            hash = sha.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return hash;

    }

    public EncryptedStruck openAccount(ByteString encodedPublicKey, int nonce) {
        Sequencemessage sequencemessage = Sequencemessage.newBuilder().setOpenAccountRequest(
                OpenAccountRequest.newBuilder().setKey(encodedPublicKey).build()).setNonce(nonce).build();
        Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage)
                .setSenderKey(encodedPublicKey).build();

        byte[] sequencemessagetoencrypt = BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes();

        return EncryptedStruck.newBuilder()
                .setEncryptedhash(ByteString.copyFrom(digitalsign(hash(sequencemessagetoencrypt), _userPrivateKey)))
                .setUnencriptedhash(unencriptedhash).build();
    }

    public OpenAccountResponse openAccountResponse(EncryptedStruck response) throws ManipulatedPackageException {
        byte[] calculatedHash = hash(BaseEncoding.base64()
                .encode(response.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(response.getUnencriptedhash().getSenderKey().toByteArray()));
        } catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = decrypt(response.getEncryptedhash().toByteArray(), publicKey);

        OpenAccountResponse accResponse = response.getUnencriptedhash().getSequencemessage().getOpenAccountResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedHash);

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }
        return OpenAccountResponse.newBuilder().setPublicKey(accResponse.getPublicKey())
                .setResponse(accResponse.getResponse()).build();
    }

    public EncryptedStruck sendAmount(String senderPublicKey, String receiverPublicKey,
            int amount, int nonce) {
        Sequencemessage sequencemessage = Sequencemessage.newBuilder().setSendAmountRequest(
                SendAmountRequest.newBuilder().setSenderKey(senderPublicKey).setReceiverKey(receiverPublicKey)
                        .setAmount(amount).build())
                .setNonce(nonce).build();
        Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage)
                .setSenderKey(ByteString.copyFrom(senderPublicKey.getBytes())).build();

        byte[] sequencemessagetoencrypt = BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes();

        return EncryptedStruck.newBuilder()
                .setEncryptedhash(ByteString.copyFrom(digitalsign(hash(sequencemessagetoencrypt), _userPrivateKey)))
                .setUnencriptedhash(unencriptedhash).build();
    }

    public SendAmountResponse sendAmountResponse(EncryptedStruck response) throws ManipulatedPackageException {
        byte[] calculatedHash = hash(BaseEncoding.base64()
                .encode(response.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(response.getUnencriptedhash().getSenderKey().toByteArray()));
        } catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = decrypt(response.getEncryptedhash().toByteArray(), publicKey);

        SendAmountResponse accResponse = response.getUnencriptedhash().getSequencemessage().getSendAmountResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedHash);

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }
        return SendAmountResponse.newBuilder().setResponse(accResponse.getResponse())
                .setResponse(accResponse.getResponse()).build();
    }

    public CheckAccountRequest checkAccount(String publicKey) {
        return CheckAccountRequest.newBuilder().setKey(publicKey).build();
    }

    public AuditRequest audit(String publicKey) {
        return AuditRequest.newBuilder().setKey(publicKey).build();
    }

    public EncryptedStruck receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId,
                                         boolean accept, int nonce) {
        Sequencemessage sequencemessage = Sequencemessage.newBuilder().setReceiveAmountRequest(
                ReceiveAmountRequest.newBuilder().setReceiverKey(receiverPublicKey)
                        .setSenderKey(senderPublicKey).setTransactionId(transactionId).setAnswer(accept).build()).setNonce(nonce).build();
        Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage)
                .setSenderKey(ByteString.copyFrom(receiverPublicKey.getBytes())).build();

        byte[] sequencemessagetoencrypt = BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes();

        return EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(digitalsign(
                hash(sequencemessagetoencrypt), _userPrivateKey))).setUnencriptedhash(unencriptedhash).build();

    }

    public ReceiveAmountResponse receiveAmountResponse(EncryptedStruck response) throws ManipulatedPackageException {
        byte[] calculatedHash = hash(BaseEncoding.base64()
                .encode(response.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec
                    (response.getUnencriptedhash().getSenderKey().toByteArray()));
        }
        catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = decrypt(response.getEncryptedhash().toByteArray(),publicKey);

        ReceiveAmountResponse accResponse = response.getUnencriptedhash().getSequencemessage().getReceiveAmountResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedHash);

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }
        return ReceiveAmountResponse.newBuilder().setResult(accResponse.getResult()).build();
    }

    public SearchKeysRequest searchKeys() {
        return SearchKeysRequest.newBuilder().build();
    }

    private byte[] digitalsign(byte[] inputhash, PrivateKey signprivatekey) {

        Cipher cipher;
        byte[] signature = null;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, signprivatekey);
            signature = cipher.doFinal(inputhash);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e) {
            e.printStackTrace();
        }

        return signature;

    }

    private byte[] decrypt(byte[] encryptedString, PublicKey publicKey) {
        Cipher cipher;
        byte[] decryptedMessageHash = null;

        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            decryptedMessageHash = cipher.doFinal(encryptedString);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e) {
            e.printStackTrace();
        }

        return decryptedMessageHash;
    }
}
