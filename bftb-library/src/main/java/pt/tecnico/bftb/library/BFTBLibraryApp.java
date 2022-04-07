package pt.tecnico.bftb.library;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

import pt.tecnico.bftb.cripto.BFTBCriptoApp;
import pt.tecnico.bftb.grpc.Bftb.AuditRequest;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.EncryptedStruck;
import pt.tecnico.bftb.grpc.Bftb.NonceRequest;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.Sequencemessage;
import pt.tecnico.bftb.grpc.Bftb.Unencriptedhash;

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

    
    public EncryptedStruck openAccount(ByteString encodedPublicKey, int nonce) {
        Sequencemessage sequencemessage = Sequencemessage.newBuilder().setOpenAccountRequest(
                OpenAccountRequest.newBuilder().setKey(encodedPublicKey).build()).setNonce(nonce).build();
        Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage)
                .setSenderKey(encodedPublicKey).build();

        byte[] sequencemessagetoencrypt = BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes();

        return EncryptedStruck.newBuilder()
                .setEncryptedhash(ByteString.copyFrom(BFTBCriptoApp.digitalsign(BFTBCriptoApp.hash(sequencemessagetoencrypt), _userPrivateKey)))
                .setUnencriptedhash(unencriptedhash).build();
    }

    public OpenAccountResponse openAccountResponse(EncryptedStruck response) throws ManipulatedPackageException {
        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64()
                .encode(response.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(response.getUnencriptedhash().getSenderKey().toByteArray()));
        } catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = BFTBCriptoApp.decrypt(response.getEncryptedhash().toByteArray(), publicKey);

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
                .setEncryptedhash(ByteString.copyFrom(BFTBCriptoApp.digitalsign(BFTBCriptoApp.hash(sequencemessagetoencrypt), _userPrivateKey)))
                .setUnencriptedhash(unencriptedhash).build();
    }

    public SendAmountResponse sendAmountResponse(EncryptedStruck response) throws ManipulatedPackageException {
        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64()
                .encode(response.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(response.getUnencriptedhash().getSenderKey().toByteArray()));
        } catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = BFTBCriptoApp.decrypt(response.getEncryptedhash().toByteArray(), publicKey);

        SendAmountResponse accResponse = response.getUnencriptedhash().getSequencemessage().getSendAmountResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedHash);

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }
        return SendAmountResponse.newBuilder().setResponse(accResponse.getResponse())
                .setResponse(accResponse.getResponse()).build();
    }

    public EncryptedStruck checkAccount(ByteString bytepublic, int nonce) {

        Sequencemessage sequencemessage = Sequencemessage.newBuilder().setCheckAccountRequest(
                CheckAccountRequest.newBuilder().setKey(bytepublic).build()).setNonce(nonce).build();
        Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage).setSenderKey(bytepublic).build();

        byte[] sequencemessagetoencrypt = BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes();

        return EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(BFTBCriptoApp.hash(sequencemessagetoencrypt))).setUnencriptedhash(unencriptedhash).build();
    }

    public CheckAccountResponse checkAccountResponse(EncryptedStruck response) throws ManipulatedPackageException{

        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64()
                .encode(response.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());


        CheckAccountResponse accResponse = response.getUnencriptedhash().getSequencemessage().getCheckAccountResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, response.getEncryptedhash().toByteArray());

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }

        return CheckAccountResponse.newBuilder().setBalance(accResponse.getBalance()).addAllPending(accResponse.getPendingList()).build();
    }

    public EncryptedStruck audit(ByteString bytepublic, int nonce) {

        Sequencemessage sequencemessage = Sequencemessage.newBuilder().setAuditRequest(
                AuditRequest.newBuilder().setKey(bytepublic).build()).setNonce(nonce).build();
        Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage).setSenderKey(bytepublic).build();

        byte[] sequencemessagetoencrypt = BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes();

        return EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(BFTBCriptoApp.hash(sequencemessagetoencrypt))).setUnencriptedhash(unencriptedhash).build();
    }

    public AuditResponse auditResponse(EncryptedStruck response) throws ManipulatedPackageException{

        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64()
                .encode(response.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());


        AuditResponse accResponse = response.getUnencriptedhash().getSequencemessage().getAuditResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, response.getEncryptedhash().toByteArray());

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }

        return AuditResponse.newBuilder().addAllSet(accResponse.getSetList()).build();
    }

    public EncryptedStruck receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId,
                                         boolean accept, int nonce) {
        Sequencemessage sequencemessage = Sequencemessage.newBuilder().setReceiveAmountRequest(
                ReceiveAmountRequest.newBuilder().setReceiverKey(receiverPublicKey)
                        .setSenderKey(senderPublicKey).setTransactionId(transactionId).setAnswer(accept).build()).setNonce(nonce).build();
        Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage)
                .setSenderKey(ByteString.copyFrom(receiverPublicKey.getBytes())).build();

        byte[] sequencemessagetoencrypt = BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes();

        return EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(BFTBCriptoApp.digitalsign(
                BFTBCriptoApp.hash(sequencemessagetoencrypt), _userPrivateKey))).setUnencriptedhash(unencriptedhash).build();

    }

    public ReceiveAmountResponse receiveAmountResponse(EncryptedStruck response) throws ManipulatedPackageException {
        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64()
                .encode(response.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec
                    (response.getUnencriptedhash().getSenderKey().toByteArray()));
        }
        catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = BFTBCriptoApp.decrypt(response.getEncryptedhash().toByteArray(),publicKey);

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
}
