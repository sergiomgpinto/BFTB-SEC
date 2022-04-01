package pt.tecnico.bftb.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.netty.shaded.io.netty.handler.codec.base64.Base64;
import io.grpc.stub.StreamObserver;
import pt.tecnico.bftb.grpc.Bftb;
import pt.tecnico.bftb.server.domain.Account;
import pt.tecnico.bftb.server.domain.Label;
import pt.tecnico.bftb.server.domain.exception.*;
import pt.tecnico.bftb.grpc.BFTBGrpc;
import pt.tecnico.bftb.server.domain.BFTBServerLogic;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.AuditRequest;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.EncryptedMessage;
import pt.tecnico.bftb.grpc.Bftb.EncryptedStruck;
import pt.tecnico.bftb.grpc.Bftb.NonceRequest;
import pt.tecnico.bftb.grpc.Bftb.NonceResponse;
import pt.tecnico.bftb.grpc.Bftb.SendAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.Unencriptedhash;
import pt.tecnico.bftb.grpc.Bftb.Sequencemessage;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;

import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static io.grpc.Status.*;

public class BFTBImpl extends BFTBGrpc.BFTBImplBase {

    private BFTBServerLogic _bftb = new BFTBServerLogic();

    PrivateKey _serverPrivateKey = null;
    PublicKey _serverPublicKey = null;

    public BFTBImpl(PrivateKey serverPrivateKey, PublicKey serverPublicKey) {
        _serverPrivateKey = serverPrivateKey;
        _serverPublicKey = serverPublicKey;
    }

    private String Encript(String normalMessage) {
        return null;
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

    private byte[] hash(byte[] inputdata) {

        byte[] hash = null;
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
            sha.update(inputdata);
            hash = sha.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return hash;

    }

    @Override
    public void getNonce(NonceRequest request, StreamObserver<NonceResponse> responseObserver) {

        NonceResponse response = null;

        if (request.getSenderKey().toString(StandardCharsets.UTF_8).contains("PublicKey")) {
            response = NonceResponse.newBuilder().setNonce(_bftb.newNonce(_bftb.searchAccount(request.getSenderKey()
                    .toString(StandardCharsets.UTF_8)).getPublicKey())).build();
        } else {
            response = NonceResponse.newBuilder().setNonce(_bftb.newNonce(request.getSenderKey())).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void openAccount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = hash(BaseEncoding.base64()
                .encode(request.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());
        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(request.getUnencriptedhash().getSenderKey().toByteArray()));
        } catch (NoSuchAlgorithmException nsae) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
            return;
        } catch (InvalidKeySpecException ikpe) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
            return;
        }

        byte[] decriptedhash = decrypt(request.getEncryptedhash().toByteArray(), publicKey);
        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedhash);

        if (request.getUnencriptedhash().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }

        try {
            String ret = _bftb.openAccount(request.getUnencriptedhash().getSenderKey());

            String[] values = ret.split(":");
            OpenAccountResponse accountResponse = null;

            if (ret.indexOf(":") != -1) {
                accountResponse = OpenAccountResponse.newBuilder().setResponse(values[0]).setPublicKey(values[1])
                        .build();
            } else {
                accountResponse = OpenAccountResponse.newBuilder().setResponse(ret).build();
            }

            Sequencemessage sequencemessage = Sequencemessage.newBuilder().setOpenAccountResponse(accountResponse)
                    .setNonce(request.getUnencriptedhash().getSequencemessage().getNonce()).build();
            Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder()
                    .setSequencemessage(sequencemessage)
                    .setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                    .build();

            response = EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(digitalSign(
                    hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes()), _serverPrivateKey)))
                    .setUnencriptedhash(unencriptedhash).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        } catch (BFTBDatabaseException bde) {
            responseObserver.onError(ABORTED.withDescription(bde.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void sendAmount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = hash(BaseEncoding.base64()
                .encode(request.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        String senderKey = request.getUnencriptedhash().getSequencemessage()
                .getSendAmountRequest().getSenderKey();

        String receiverKey = request.getUnencriptedhash().getSequencemessage()
                .getSendAmountRequest().getReceiverKey();

        PublicKey senderPubKey;
        PublicKey receiverPubKey;
        int amount = request.getUnencriptedhash().getSequencemessage().getSendAmountRequest().getAmount();

        senderPubKey = _bftb.searchAccount(request.getUnencriptedhash().getSequencemessage()
                .getSendAmountRequest().getSenderKey()).getPublicKey();

        receiverPubKey = _bftb.searchAccount(request.getUnencriptedhash().getSequencemessage()
                .getSendAmountRequest().getSenderKey()).getPublicKey();

        byte[] decriptedhash = decrypt(request.getEncryptedhash().toByteArray(), senderPubKey);
        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedhash);

        if (request.getUnencriptedhash().getSenderKey() == null) {
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

        try {

            SendAmountResponse ret = SendAmountResponse.newBuilder()
                    .setResponse(_bftb.sendAmount(senderKey, receiverKey, amount))
                    .build();
            Sequencemessage sequencemessage = Sequencemessage.newBuilder().setSendAmountResponse(ret)
                    .setNonce(request.getUnencriptedhash().getSequencemessage().getNonce()).build();
            Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder()
                    .setSequencemessage(sequencemessage)
                    .setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                    .build();

            response = EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(digitalSign(
                    hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes()), _serverPrivateKey)))
                    .setUnencriptedhash(unencriptedhash).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        } catch (NoAccountException nae) {
            responseObserver.onError(ABORTED.withDescription(nae.getMessage()).asRuntimeException());
        } catch (BFTBDatabaseException bde) {
            responseObserver.onError(ABORTED.withDescription(bde.getMessage()).asRuntimeException());
        }

    }

    @Override
    public void checkAccount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = hash(BaseEncoding.base64()
                .encode(request.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, request.getEncryptedhash().toByteArray());

        if (request.getUnencriptedhash().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }

        CheckAccountResponse checkresponse = null;
        try {

            List<String> ret = _bftb.checkAccount(
                    request.getUnencriptedhash().getSequencemessage().getCheckAccountRequest().getKey().toStringUtf8());

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
            Sequencemessage sequencemessage = Sequencemessage.newBuilder().setCheckAccountResponse(checkresponse)
                    .setNonce(request.getUnencriptedhash().getSequencemessage().getNonce()).build();
            Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage)
                    .setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded())).build();

            response = EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(
                    hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes())))
                    .setUnencriptedhash(unencriptedhash).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NonExistentAccount nea) {
            responseObserver.onError(ABORTED.withDescription(nea.getMessage()).asRuntimeException());
        }

    }

    @Override
    public void receiveAmount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = hash(BaseEncoding.base64()
                .encode(request.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        PublicKey publicKey = _bftb.searchAccount(request.getUnencriptedhash()
                .getSequencemessage().getReceiveAmountRequest().getReceiverKey()).getPublicKey();

        byte[] decriptedhash = decrypt(request.getEncryptedhash().toByteArray(), publicKey);
        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedhash);

        if (request.getUnencriptedhash().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }
        boolean answer = request.getUnencriptedhash().getSequencemessage().getReceiveAmountRequest().getAnswer();
        String receiverKey = request.getUnencriptedhash().getSequencemessage().getReceiveAmountRequest().getReceiverKey();
        String senderKey = request.getUnencriptedhash().getSequencemessage().getReceiveAmountRequest().getSenderKey();
        int transactionId = request.getUnencriptedhash().getSequencemessage().getReceiveAmountRequest().getTransactionId();

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
        //here
        try {
            ReceiveAmountResponse logicResponse = ReceiveAmountResponse.newBuilder()
                    .setResult(_bftb.receiveAmount(receiverKey, senderKey, transactionId, answer)).build();

            Sequencemessage sequencemessage = Sequencemessage.newBuilder().setReceiveAmountResponse(logicResponse)
                    .setNonce(request.getUnencriptedhash().getSequencemessage().getNonce()).build();
            Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder()
                    .setSequencemessage(sequencemessage)
                    .setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                    .build();

            response = EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(digitalSign(
                            hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes()), _serverPrivateKey)))
                    .setUnencriptedhash(unencriptedhash).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NonExistentAccount nea) {
            responseObserver.onError(ABORTED.withDescription(nea.getMessage()).asRuntimeException());
        } catch (NonExistentTransaction net) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(net.getMessage()).asRuntimeException());
        } catch (NoAuthorization na) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(na.getMessage()).asRuntimeException());
        } catch (BFTBDatabaseException bde) {
            responseObserver.onError(ABORTED.withDescription(bde.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void audit(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = hash(BaseEncoding.base64().encode(request.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, request.getEncryptedhash().toByteArray());

        if (request.getUnencriptedhash().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }        
        try {
            AuditResponse auditResponse = AuditResponse.newBuilder().addAllSet(_bftb.audit(request.getUnencriptedhash().getSequencemessage().getCheckAccountRequest().getKey().toStringUtf8())).build();
            
            Sequencemessage sequencemessage = Sequencemessage.newBuilder().setAuditResponse(auditResponse).setNonce(request.getUnencriptedhash().getSequencemessage().getNonce()).build();
            Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage).setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded())).build();

            response = EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(
                    hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes())))
                    .setUnencriptedhash(unencriptedhash).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NonExistentAccount nea) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(nea.getMessage()).asRuntimeException());
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        }

    }

    public void searchKeys(SearchKeysRequest request, StreamObserver<SearchKeysResponse> responseObserver) {

        SearchKeysResponse response = SearchKeysResponse.newBuilder().addAllResult(_bftb.getAllPublicKeys()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private byte[] digitalSign(byte[] inputHash, PrivateKey signPrivateKey) {

        Cipher cipher;
        byte[] signature = null;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, signPrivateKey);
            signature = cipher.doFinal(inputHash);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e) {
            e.printStackTrace();
        }

        return signature;

    }
}