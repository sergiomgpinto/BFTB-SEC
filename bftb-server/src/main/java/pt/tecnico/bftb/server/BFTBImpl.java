package pt.tecnico.bftb.server;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.bftb.cripto.BFTBCripto;
import pt.tecnico.bftb.grpc.BFTBGrpc;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.EncryptedStruck;
import pt.tecnico.bftb.grpc.Bftb.RawData;
import pt.tecnico.bftb.grpc.Bftb.NonceRequest;
import pt.tecnico.bftb.grpc.Bftb.NonceResponse;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.server.domain.Account;
import pt.tecnico.bftb.server.domain.BFTBServerLogic;
import pt.tecnico.bftb.server.domain.Label;
import pt.tecnico.bftb.server.domain.exception.*;

import static io.grpc.Status.*;

public class BFTBImpl extends BFTBGrpc.BFTBImplBase {

    private BFTBServerLogic _bftb = new BFTBServerLogic();

    PrivateKey _serverPrivateKey = null;
    PublicKey _serverPublicKey = null;

    public BFTBImpl(PrivateKey serverPrivateKey, PublicKey serverPublicKey) {
        _serverPrivateKey = serverPrivateKey;
        _serverPublicKey = serverPublicKey;
    }

    /**************************** Protocol Messages ***********************************/
    // getNonce
    // Since it also modifies states messages will be signed.
    @Override
    public void getNonce(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(request.getRawData().toByteArray()).getBytes());

        if (request.getRawData().getNonceRequest().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        PublicKey publicKey = null;
        NonceResponse response = null;
        System.out.println(request.getRawData().getNonceRequest().getSenderKey().toString(StandardCharsets.UTF_8));
        try {
            if (request.getRawData().getNonceRequest().getSenderKey().toString(StandardCharsets.UTF_8).contains("PublicKey")) {
                try {
                    publicKey = _bftb.searchAccount(request.getRawData().getNonceRequest().getSenderKey()
                                    .toString(StandardCharsets.UTF_8))
                            .getPublicKey();
                }
                catch (NonExistentAccount nea) {
                    // Should never happen.
                }
            }
            else {
                publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(request
                        .getRawData()
                        .getNonceRequest()
                        .getSenderKey()
                        .toByteArray()));
            }
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("entrei");
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
            return;
        } catch (InvalidKeySpecException ikpe) {
            System.out.println("entre2");
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
            return;
        }

        byte[] decryptedHash = BFTBCripto.decryptDigitalSignature(request.getDigitalSignature().toByteArray(), publicKey);

        boolean isCorrect = Arrays.equals(calculatedHash, decryptedHash);

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }

        if (Context.current().isCancelled()) {
            responseObserver.onError(CANCELLED.withDescription("Client canceled request.").asRuntimeException());
            return;
        }

        if (request.getRawData().getNonceRequest().getSenderKey().toString(StandardCharsets.UTF_8).contains("PublicKey")) {
            try {
                response = NonceResponse.newBuilder().setNonce(_bftb.newNonce(_bftb.searchAccount(request.getRawData()
                                .getNonceRequest().getSenderKey()
                        .toString(StandardCharsets.UTF_8)).getPublicKey()))
                        .setServerPublicKey(ByteString.copyFrom(_serverPublicKey.getEncoded())).build();
            } catch (NonExistentAccount e) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
                return;
            }
        } else {
            response = NonceResponse.newBuilder().setNonce(_bftb.newNonce(request.getRawData()
                            .getNonceRequest().getSenderKey()))
                    .setServerPublicKey(ByteString.copyFrom(_serverPublicKey.getEncoded())).build();
        }

        RawData rawData = RawData.newBuilder().setNonceResponse(response).build();

         EncryptedStruck encryptedResponse = EncryptedStruck.newBuilder().setDigitalSignature(ByteString.copyFrom(BFTBCripto.digitalSign(
                        BFTBCripto.hash(BaseEncoding.base64().encode(rawData.toByteArray()).getBytes()), _serverPrivateKey)))
                .setRawData(rawData).build();

        responseObserver.onNext(encryptedResponse);
        responseObserver.onCompleted();
    }

    /**************************** Read Only Operations ***********************************/
    // checkAccount
    // audit
    // searchKeys

    @Override
    public void checkAccount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(request.getRawData().toByteArray()).getBytes());

        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, request.getDigest().toByteArray());

        if (request.getRawData().getCheckAccountRequest().getKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }

        String userKey = request.getRawData().getCheckAccountRequest().getUserKey();
        int correctStoredNonce = _bftb.getUserNonce(userKey);
        int receivedNonce = request.getRawData().getNonce();

        if (receivedNonce != correctStoredNonce) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.REPLAY_ATTACK).asRuntimeException());
            return;
        }

        if (Context.current().isCancelled()) {
            responseObserver.onError(CANCELLED.withDescription("Client canceled request.").asRuntimeException());
            return;
        }

        CheckAccountResponse checkresponse = null;
        try {

            List<String> ret = _bftb.checkAccount(
                    request.getRawData().getCheckAccountRequest().getKey().toStringUtf8());

            // Owner of the account has no pending transactions.
            if (ret.size() == 1) {
                List<String> pending = new ArrayList<>();
                pending.add(Label.NO_PENDING_TRANSACTIONS);
                checkresponse = CheckAccountResponse.newBuilder().setBalance(Integer.parseInt(ret.get(0)))
                        .addAllPending(pending).build();
            } else {
                checkresponse = CheckAccountResponse.newBuilder().setBalance(Integer.parseInt(ret.get(0)))
                        .addAllPending(ret.subList(1, ret.size())).build();
            }
            RawData rawData = RawData.newBuilder().setCheckAccountResponse(checkresponse).setNonce(request.getRawData().getNonce()).build();

            response = EncryptedStruck.newBuilder().setDigest(ByteString.copyFrom(
                            BFTBCripto.hash(BaseEncoding.base64().encode(rawData.toByteArray()).getBytes())))
                    .setRawData(rawData).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NonExistentAccount nea) {
            responseObserver.onError(ABORTED.withDescription(nea.getMessage()).asRuntimeException());
        }

    }

    @Override
    public void audit(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64().encode(request.getRawData().toByteArray()).getBytes());

        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, request.getDigest().toByteArray());

        if (request.getRawData().getAuditRequest().getKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }

        String userKey = request.getRawData().getAuditRequest().getUserKey();
        int correctStoredNonce = _bftb.getUserNonce(userKey);
        int receivedNonce = request.getRawData().getNonce();

        if (receivedNonce != correctStoredNonce) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.REPLAY_ATTACK).asRuntimeException());
            return;
        }

        if (Context.current().isCancelled()) {
            responseObserver.onError(CANCELLED.withDescription("Client canceled request.").asRuntimeException());
            return;
        }

        try {
            AuditResponse auditResponse = AuditResponse.newBuilder().addAllSet(_bftb.audit(request.getRawData().getAuditRequest().getKey().toStringUtf8())).build();

            RawData rawData = RawData.newBuilder().setAuditResponse(auditResponse).setNonce(request.getRawData().getNonce()).build();

            response = EncryptedStruck.newBuilder().setDigest(ByteString.copyFrom(
                            BFTBCripto.hash(BaseEncoding.base64().encode(rawData.toByteArray()).getBytes())))
                    .setRawData(rawData).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NonExistentAccount nea) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(nea.getMessage()).asRuntimeException());
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        }
    }

    @Override
    public void searchKeys(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64().encode(request.getRawData().toByteArray()).getBytes());

        boolean isCorrect = Arrays.equals(calculatedHash, request.getDigest().toByteArray());

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }

        String userKey = request.getRawData().getSearchKeyRequest().getUserKey();
        int correctStoredNonce = _bftb.getUserNonce(userKey);
        int receivedNonce = request.getRawData().getNonce();

        if (receivedNonce != correctStoredNonce) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.REPLAY_ATTACK).asRuntimeException());
            return;
        }

        if (Context.current().isCancelled()) {
            responseObserver.onError(CANCELLED.withDescription("Client canceled request.").asRuntimeException());
            return;
        }

        SearchKeysResponse searchKeysResponse = SearchKeysResponse.newBuilder().addAllResult(_bftb.getAllPublicKeys()).build();

        RawData rawData = RawData.newBuilder().setSearchKeyResponse(searchKeysResponse).setNonce(request.getRawData().getNonce()).build();

        EncryptedStruck response = EncryptedStruck.newBuilder().setDigest(ByteString.copyFrom(
                        BFTBCripto.hash(BaseEncoding.base64().encode(rawData.toByteArray()).getBytes())))
                .setRawData(rawData).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**************************** Write Operations ***********************************/
    // openAccount
    // sendAmount
    // receiveAmount

    @Override
    public void openAccount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(request.getRawData().toByteArray()).getBytes());
        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(request.getRawData().getOpenAccountRequest().getKey().toByteArray()));
        } catch (NoSuchAlgorithmException nsae) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
            return;
        } catch (InvalidKeySpecException ikpe) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
            return;
        }

        byte[] decryptedHash = BFTBCripto.decryptDigitalSignature(request.getDigitalSignature().toByteArray(), publicKey);
        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, decryptedHash);

        if (request.getRawData().getOpenAccountRequest().getKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }

        ByteString userKey = request.getRawData().getOpenAccountRequest().getKey();
        Account account = null;
        boolean doesAccountExist = true;

        try {
            PublicKey localPublicKey = null;
            try {
                localPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(userKey.toByteArray()));
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
            }
            account = _bftb.searchAccount(localPublicKey);

        }
        catch (NonExistentAccount nea) {
            doesAccountExist = false;
        }

        // If account already exists this code is executed.
        if (doesAccountExist) {
            int correctStoredNonce = _bftb.getUserNonce(account.getPublicKeyString());

            int receivedNonce = request.getRawData().getNonce();

            if (receivedNonce != correctStoredNonce) {
                responseObserver.onError(PERMISSION_DENIED.withDescription(Label.REPLAY_ATTACK).asRuntimeException());
                return;
            }
        }

        if (Context.current().isCancelled()) {
            responseObserver.onError(CANCELLED.withDescription("Client canceled request.").asRuntimeException());
            return;
        }

        try {
            String ret = _bftb.openAccount(request.getRawData().getOpenAccountRequest().getKey(),
                    request.getRawData().getOpenAccountRequest().getUsername());

            String[] values = ret.split(":");
            OpenAccountResponse accountResponse = null;

            if (ret.indexOf(":") != -1) {// Enters here if user had not registered an account yet.
                accountResponse = OpenAccountResponse.newBuilder().setResponse(values[0])
                        .setPublicKey(values[1])
                        .setServerPublicKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                        .build();
            } else {// Enters here if user has already a registered account.
                accountResponse = OpenAccountResponse.newBuilder().setResponse(ret)
                        .setPublicKey(_bftb.searchAccount(publicKey).getPublicKeyString())
                        .setServerPublicKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                        .build();
            }

            RawData rawData = RawData.newBuilder().setOpenAccountResponse(accountResponse)
                    .setNonce(request.getRawData().getNonce()).build();

            response = EncryptedStruck.newBuilder().setDigitalSignature(ByteString.copyFrom(BFTBCripto.digitalSign(
                            BFTBCripto.hash(BaseEncoding.base64().encode(rawData.toByteArray()).getBytes()), _serverPrivateKey)))
                    .setRawData(rawData).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        } catch (NonExistentAccount nea) {
           System.out.println(nea.getMessage());
        }
    }

    @Override
    public void sendAmount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(request.getRawData().toByteArray()).getBytes());

        String senderKey = request.getRawData().getSendAmountRequest().getSenderKey();

        String receiverKey = request.getRawData().getSendAmountRequest().getReceiverKey();

        PublicKey senderPubKey;
        PublicKey receiverPubKey;
        int amount = request.getRawData().getSendAmountRequest().getAmount();

        try {
            senderPubKey = _bftb.searchAccount(request.getRawData().getSendAmountRequest().getSenderKey()).getPublicKey();
            receiverPubKey = _bftb.searchAccount(request.getRawData().getSendAmountRequest().getSenderKey()).getPublicKey();
        } catch (NonExistentAccount e1) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        byte[] decryptedHash = BFTBCripto.decryptDigitalSignature(request.getDigitalSignature().toByteArray(), senderPubKey);
        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, decryptedHash);

        if (request.getRawData().getSendAmountRequest().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }

        if (senderKey == null || senderKey.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }
        if (receiverKey == null || receiverKey.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }
        if (senderKey.equals(receiverKey)) {
            responseObserver
                    .onError(INVALID_ARGUMENT.withDescription(Label.INVALID_ARGS_SEND_AMOUNT).asRuntimeException());
            return;
        }
        if (amount <= 0) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_AMOUNT).asRuntimeException());
            return;
        }

        String userKey = request.getRawData().getSendAmountRequest().getSenderKey();
        int correctStoredNonce = _bftb.getUserNonce(userKey);
        int receivedNonce = request.getRawData().getNonce();

        if (receivedNonce != correctStoredNonce) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.REPLAY_ATTACK).asRuntimeException());
            return;
        }

        if (Context.current().isCancelled()) {
            responseObserver.onError(CANCELLED.withDescription("Client canceled request.").asRuntimeException());
            return;
        }

        try {

            SendAmountResponse ret = SendAmountResponse.newBuilder()
                    .setResponse(_bftb.sendAmount(senderKey, receiverKey, amount))
                    .setServerPublicKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                    .build();
            
            RawData rawData = RawData.newBuilder().setSendAmountResponse(ret).setNonce(request.getRawData().getNonce()).build();

            response = EncryptedStruck.newBuilder().setDigitalSignature(ByteString.copyFrom(BFTBCripto.digitalSign(
                            BFTBCripto.hash(BaseEncoding.base64().encode(rawData.toByteArray()).getBytes()), _serverPrivateKey)))
                    .setRawData(rawData).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        } catch (NoAccountException nae) {
            responseObserver.onError(ABORTED.withDescription(nae.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void receiveAmount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(request.getRawData().toByteArray()).getBytes());

        PublicKey publicKey;
        try {
            publicKey = _bftb.searchAccount(request.getRawData().getReceiveAmountRequest().getReceiverKey()).getPublicKey();
        } catch (NonExistentAccount e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        byte[] decriptedhash = BFTBCripto.decryptDigitalSignature(request.getDigitalSignature().toByteArray(), publicKey);
        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedhash);

        if (request.getRawData().getReceiveAmountRequest().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }
        boolean answer = request.getRawData().getReceiveAmountRequest().getAnswer();
        String receiverKey = request.getRawData().getReceiveAmountRequest().getReceiverKey();
        String senderKey = request.getRawData().getReceiveAmountRequest().getSenderKey();
        int transactionId = request.getRawData().getReceiveAmountRequest().getTransactionId();

        if (receiverKey == null || receiverKey.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }
        if (senderKey == null || senderKey.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }
        if (senderKey.equals(receiverKey)) {
            responseObserver
                    .onError(INVALID_ARGUMENT.withDescription(Label.INVALID_ARGS_SEND_AMOUNT).asRuntimeException());
            return;
        }
        if (transactionId <= 0) {
            responseObserver
                    .onError(INVALID_ARGUMENT.withDescription(Label.INVALID_TRANSACTION_ID).asRuntimeException());
            return;
        }

        String userKey = request.getRawData().getReceiveAmountRequest().getReceiverKey();
        int correctStoredNonce = _bftb.getUserNonce(userKey);
        int receivedNonce = request.getRawData().getNonce();

        if (receivedNonce != correctStoredNonce) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.REPLAY_ATTACK).asRuntimeException());
            return;
        }

        if (Context.current().isCancelled()) {
            responseObserver.onError(CANCELLED.withDescription("Client canceled request.").asRuntimeException());
            return;
        }

        try {
            ReceiveAmountResponse logicResponse = ReceiveAmountResponse.newBuilder()
                    .setResult(_bftb.receiveAmount(receiverKey, senderKey, transactionId, answer))
                    .setServerPublicKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                    .build();

            RawData rawData = RawData.newBuilder().setReceiveAmountResponse(logicResponse)
                    .setNonce(request.getRawData().getNonce()).build();

            response = EncryptedStruck.newBuilder().setDigitalSignature(ByteString.copyFrom(BFTBCripto.digitalSign(
                BFTBCripto.hash(BaseEncoding.base64().encode(rawData.toByteArray()).getBytes()), _serverPrivateKey)))
                    .setRawData(rawData).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NonExistentAccount nea) {
            responseObserver.onError(ABORTED.withDescription(nea.getMessage()).asRuntimeException());
        } catch (NonExistentTransaction net) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(net.getMessage()).asRuntimeException());
        } catch (NoAuthorization na) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(na.getMessage()).asRuntimeException());
        }
    }

}