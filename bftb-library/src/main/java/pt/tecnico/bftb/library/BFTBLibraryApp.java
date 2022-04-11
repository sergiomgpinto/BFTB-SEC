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
import pt.tecnico.bftb.grpc.Bftb.Data;;

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

        Data data = Data.newBuilder()
                        .setOpenAccountRequest(openAccountRequest)
                        .setNonce(nonce)
                        .build();

        RawData rawData = RawData.newBuilder()
                        .setData(data)
                        .setSenderKey(encodedPublicKey)
                        .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(data.toByteArray()).getBytes();

        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash(rawDataBytes), _userPrivateKey));

        return EncryptedStruck.newBuilder()
                .setDigitalSignature(digitalSignature)
                .setRawData(rawData)
                .build();
    }

    public OpenAccountResponse openAccountResponse(EncryptedStruck response) throws ManipulatedPackageException {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().getData().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(response.getRawData().getSenderKey().toByteArray()));
        } catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(), publicKey);

        OpenAccountResponse accResponse = response.getRawData().getData().getOpenAccountResponse();

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

        Data data = Data.newBuilder()
                        .setSendAmountRequest(sendAmountRequest)
                        .setNonce(nonce)
                        .build();

        RawData rawData = RawData.newBuilder()
                        .setData(data)
                        .setSenderKey(ByteString.copyFrom(senderPublicKey.getBytes()))
                        .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(data.toByteArray()).getBytes();

        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash(rawDataBytes), _userPrivateKey));

        return EncryptedStruck.newBuilder()
                .setDigitalSignature(digitalSignature)
                .setRawData(rawData)
                .build();
    }

    public SendAmountResponse sendAmountResponse(EncryptedStruck response) throws ManipulatedPackageException {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().getData().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(response.getRawData().getSenderKey().toByteArray()));
        } catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(), publicKey);

        SendAmountResponse accResponse = response.getRawData().getData().getSendAmountResponse();

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

        Data data = Data.newBuilder()
                        .setCheckAccountRequest(checkAccountRequest)
                        .setNonce(nonce)
                        .build();

        RawData rawData = RawData.newBuilder()
                        .setData(data)
                        .setSenderKey(bytepublic)
                        .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(data.toByteArray()).getBytes();

        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.hash(rawDataBytes));

        return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();
    }

    public CheckAccountResponse checkAccountResponse(EncryptedStruck response) throws ManipulatedPackageException{
        System.out.println("entrada checkacount libray");
        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().getData().toByteArray()).getBytes());


        CheckAccountResponse accResponse = response.getRawData().getData().getCheckAccountResponse();

        boolean isCorrect = Arrays.equals(calculatedHash, response.getDigitalSignature().toByteArray());

        if (!isCorrect) {
            throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                    " was sent.");
        }

        return CheckAccountResponse.newBuilder().setBalance(accResponse.getBalance()).addAllPending(accResponse.getPendingList()).build();
    }

    public EncryptedStruck audit(ByteString bytepublic, int nonce) {

        AuditRequest auditRequest = AuditRequest.newBuilder().setKey(bytepublic).build();

        Data data = Data.newBuilder()
                        .setAuditRequest(auditRequest)
                        .setNonce(nonce)
                        .build();

        RawData rawData = RawData.newBuilder()
                                .setData(data)
                                .setSenderKey(bytepublic)
                                .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(data.toByteArray()).getBytes();

        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.hash(rawDataBytes));

        return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();
    }

    public AuditResponse auditResponse(EncryptedStruck response) throws ManipulatedPackageException{

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().getData().toByteArray()).getBytes());


        AuditResponse accResponse = response.getRawData().getData().getAuditResponse();

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

        Data data = Data.newBuilder()
                        .setReceiveAmountRequest(receiveAmountRequest)
                        .setNonce(nonce)
                        .build();

        RawData rawData = RawData.newBuilder()
                        .setData(data)
                        .setSenderKey(ByteString.copyFrom(receiverPublicKey.getBytes()))
                        .build();

        byte[] rawDataBytes = BaseEncoding.base64().encode(data.toByteArray()).getBytes();
        
        ByteString digitalSignature = ByteString.copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash        (rawDataBytes), _userPrivateKey));

        return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();

    }

    public ReceiveAmountResponse receiveAmountResponse(EncryptedStruck response) throws ManipulatedPackageException {
        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(response.getRawData().getData().toByteArray()).getBytes());

        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec
                    (response.getRawData().getSenderKey().toByteArray()));
        }
        catch (Exception e) {
            System.out.println(e);
        }

        byte[] decriptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(),publicKey);

        ReceiveAmountResponse accResponse = response.getRawData().getData().getReceiveAmountResponse();

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
