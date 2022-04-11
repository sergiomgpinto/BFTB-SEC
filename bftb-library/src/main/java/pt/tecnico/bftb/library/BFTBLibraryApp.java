package pt.tecnico.bftb.library;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

import pt.tecnico.bftb.cripto.BFTBCripto;
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
import pt.tecnico.bftb.grpc.Bftb.RawData;

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

        OpenAccountRequest openAccountRequest = OpenAccountRequest.newBuilder()
                                                        .setKey(encodedPublicKey)
                                                        .build();

        RawData rawData = RawData.newBuilder()
                        .setOpenAccountRequest(openAccountRequest)
                        .setNonce(nonce)
                        .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();

        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash(rawDataBytes), _userPrivateKey));

        return EncryptedStruck.newBuilder()
                .setDigitalSignature(digitalSignature)
                .setRawData(rawData)
                .build();
    }

    public OpenAccountResponse openAccountResponse(EncryptedStruck response) throws ManipulatedPackageException {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(response.getRawData().getOpenAccountResponse().getPublicKey().getBytes()));  //get encoded pubKey for everyone??
        } catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(), publicKey);

        OpenAccountResponse accResponse = response.getRawData().getOpenAccountResponse();

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

        SendAmountRequest sendAmountRequest = SendAmountRequest.newBuilder()
                                                .setSenderKey(senderPublicKey)
                                                .setReceiverKey(receiverPublicKey)
                                                .setAmount(amount)
                                                .build();

        RawData rawData = RawData.newBuilder()
                        .setSendAmountRequest(sendAmountRequest)
                        .setNonce(nonce)
                        .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();

        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash(rawDataBytes), _userPrivateKey));

        return EncryptedStruck.newBuilder()
                .setDigitalSignature(digitalSignature)
                .setRawData(rawData)
                .build();
    }

    public SendAmountResponse sendAmountResponse(EncryptedStruck response) throws ManipulatedPackageException {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(response.getRawData().getSendAmountRequest().getSenderKey().getBytes())); //get the responses!!!
        } catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(), publicKey);

        SendAmountResponse accResponse = response.getRawData().getSendAmountResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedHash);

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }
        return SendAmountResponse.newBuilder().setResponse(accResponse.getResponse())
                .setResponse(accResponse.getResponse()).build();
    }

    public EncryptedStruck checkAccount(ByteString bytepublic, int nonce) {

        CheckAccountRequest checkAccountRequest = CheckAccountRequest.newBuilder()
                        .setKey(bytepublic)
                        .build();

        RawData rawData = RawData.newBuilder()
                        .setCheckAccountRequest(checkAccountRequest)
                        .setNonce(nonce)
                        .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();

        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.hash(rawDataBytes));

        return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();
    }

    public CheckAccountResponse checkAccountResponse(EncryptedStruck response) throws ManipulatedPackageException{
        System.out.println("entrada checkacount libray");
        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().toByteArray()).getBytes());


        CheckAccountResponse accResponse = response.getRawData().getCheckAccountResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, response.getDigitalSignature().toByteArray());

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }

        return CheckAccountResponse.newBuilder().setBalance(accResponse.getBalance()).addAllPending(accResponse.getPendingList()).build();
    }

    public EncryptedStruck audit(ByteString bytepublic, int nonce) {

        AuditRequest auditRequest = AuditRequest.newBuilder().setKey(bytepublic).build();

        RawData rawData = RawData.newBuilder()
                        .setAuditRequest(auditRequest)
                        .setNonce(nonce)
                        .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();

        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.hash(rawDataBytes));

        return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();
    }

    public AuditResponse auditResponse(EncryptedStruck response) throws ManipulatedPackageException{

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().toByteArray()).getBytes());


        AuditResponse accResponse = response.getRawData().getAuditResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, response.getDigitalSignature().toByteArray());

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }

        return AuditResponse.newBuilder().addAllSet(accResponse.getSetList()).build();
    }

    public EncryptedStruck receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId, boolean accept, int nonce) {

        ReceiveAmountRequest receiveAmountRequest = ReceiveAmountRequest.newBuilder()
                                                        .setReceiverKey(receiverPublicKey)
                                                        .setSenderKey(senderPublicKey)
                                                        .setTransactionId(transactionId)
                                                        .setAnswer(accept)
                                                        .build();

        RawData rawData = RawData.newBuilder()
                        .setReceiveAmountRequest(receiveAmountRequest)
                        .setNonce(nonce)
                        .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();
        
        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash(rawDataBytes), _userPrivateKey));

        return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();

    }

    public ReceiveAmountResponse receiveAmountResponse(EncryptedStruck response) throws ManipulatedPackageException {
        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec
                    (response.getRawData().getReceiveAmountRequest().getSenderKey().getBytes())); //get response!!
        }
        catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(),publicKey);

        ReceiveAmountResponse accResponse = response.getRawData().getReceiveAmountResponse();

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
