package pt.tecnico.bftb.server;

import static io.grpc.Status.ABORTED;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.PERMISSION_DENIED;
import static io.grpc.Status.UNKNOWN;

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

import io.grpc.stub.StreamObserver;
import pt.tecnico.bftb.cripto.BFTBCripto;
import pt.tecnico.bftb.grpc.BFTBGrpc;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.EncryptedStruck;
import pt.tecnico.bftb.grpc.Bftb.Data;
import pt.tecnico.bftb.grpc.Bftb.RawData;
import pt.tecnico.bftb.grpc.Bftb.NonceRequest;
import pt.tecnico.bftb.grpc.Bftb.NonceResponse;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.server.domain.BFTBServerLogic;
import pt.tecnico.bftb.server.domain.Label;
import pt.tecnico.bftb.server.domain.exception.BFTBDatabaseException;
import pt.tecnico.bftb.server.domain.exception.NoAccountException;
import pt.tecnico.bftb.server.domain.exception.NoAuthorization;
import pt.tecnico.bftb.server.domain.exception.NonExistentAccount;
import pt.tecnico.bftb.server.domain.exception.NonExistentTransaction;

public class BFTBImpl extends BFTBGrpc.BFTBImplBase {

    private BFTBServerLogic _bftb = new BFTBServerLogic();

    PrivateKey _serverPrivateKey = null;
    PublicKey _serverPublicKey = null;

    public BFTBImpl(PrivateKey serverPrivateKey, PublicKey serverPublicKey) {
        _serverPrivateKey = serverPrivateKey;
        _serverPublicKey = serverPublicKey;
    }
    
    @Override
    public void getNonce(NonceRequest request, StreamObserver<NonceResponse> responseObserver) {

        NonceResponse response = null;

        if (request.getSenderKey().toString(StandardCharsets.UTF_8).contains("PublicKey")) {
            try {
                response = NonceResponse.newBuilder().setNonce(_bftb.newNonce(_bftb.searchAccount(request.getSenderKey()
                        .toString(StandardCharsets.UTF_8)).getPublicKey())).build();
            } catch (NonExistentAccount e) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
                return;
            }
        } else {
            response = NonceResponse.newBuilder().setNonce(_bftb.newNonce(request.getSenderKey())).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void openAccount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(request.getRawData().getData().toByteArray()).getBytes());
        PublicKey publicKey = null;

        try {
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(request.getRawData().getSenderKey().toByteArray()));
        } catch (NoSuchAlgorithmException nsae) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
            return;
        } catch (InvalidKeySpecException ikpe) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
            return;
        }

        byte[] decriptedhash = BFTBCripto.decryptDigitalSignature(request.getDigitalSignature().toByteArray(), publicKey);
        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedhash);

        if (request.getRawData().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }

        try {
            String ret = _bftb.openAccount(request.getRawData().getSenderKey());

            String[] values = ret.split(":");
            OpenAccountResponse accountResponse = null;

            if (ret.indexOf(":") != -1) {
                accountResponse = OpenAccountResponse.newBuilder().setResponse(values[0]).setPublicKey(values[1])
                        .build();
            } else {
                accountResponse = OpenAccountResponse.newBuilder().setResponse(ret).build();
            }

            Data sequencemessage = Data.newBuilder().setOpenAccountResponse(accountResponse)
                    .setNonce(request.getRawData().getData().getNonce()).build();
            RawData unencriptedhash = RawData.newBuilder()
                    .setData(sequencemessage)
                    .setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                    .build();

            response = EncryptedStruck.newBuilder().setDigitalSignature(ByteString.copyFrom(BFTBCripto.digitalSign(
                BFTBCripto.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes()), _serverPrivateKey)))
                    .setRawData(unencriptedhash).build();

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

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(request.getRawData().getData().toByteArray()).getBytes());

        String senderKey = request.getRawData().getData()
                .getSendAmountRequest().getSenderKey();

        String receiverKey = request.getRawData().getData()
                .getSendAmountRequest().getReceiverKey();

        PublicKey senderPubKey;
        PublicKey receiverPubKey;
        int amount = request.getRawData().getData().getSendAmountRequest().getAmount();

        try {
            senderPubKey = _bftb.searchAccount(request.getRawData().getData()
                    .getSendAmountRequest().getSenderKey()).getPublicKey();
            receiverPubKey = _bftb.searchAccount(request.getRawData().getData()
                .getSendAmountRequest().getSenderKey()).getPublicKey();
        } catch (NonExistentAccount e1) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        

        byte[] decriptedhash = BFTBCripto.decryptDigitalSignature(request.getDigitalSignature().toByteArray(), senderPubKey);
        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedhash);

        if (request.getRawData().getSenderKey() == null) {
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
            Data sequencemessage = Data.newBuilder().setSendAmountResponse(ret)
                    .setNonce(request.getRawData().getData().getNonce()).build();
            RawData unencriptedhash = RawData.newBuilder()
                    .setData(sequencemessage)
                    .setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                    .build();

            response = EncryptedStruck.newBuilder().setDigitalSignature(ByteString.copyFrom(BFTBCripto.digitalSign(
                BFTBCripto.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes()), _serverPrivateKey)))
                    .setRawData(unencriptedhash).build();

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

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(request.getRawData().getData().toByteArray()).getBytes());

        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, request.getDigitalSignature().toByteArray());
        System.out.println("iscorrect");
        if (request.getRawData().getSenderKey() == null) {
            System.out.println("null");
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
                    request.getRawData().getData().getCheckAccountRequest().getKey().toStringUtf8());

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
            Data sequencemessage = Data.newBuilder().setCheckAccountResponse(checkresponse)
                    .setNonce(request.getRawData().getData().getNonce()).build();
            RawData unencriptedhash = RawData.newBuilder().setData(sequencemessage)
                    .setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded())).build();

            response = EncryptedStruck.newBuilder().setDigitalSignature(ByteString.copyFrom(
                BFTBCripto.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes())))
                    .setRawData(unencriptedhash).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NonExistentAccount nea) {
            System.out.println("testecatch");
            responseObserver.onError(ABORTED.withDescription(nea.getMessage()).asRuntimeException());
        }

    }

    @Override
    public void receiveAmount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                .encode(request.getRawData().getData().toByteArray()).getBytes());

        PublicKey publicKey;
        try {
            publicKey = _bftb.searchAccount(request.getRawData()
                    .getData().getReceiveAmountRequest().getReceiverKey()).getPublicKey();
        } catch (NonExistentAccount e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        byte[] decriptedhash = BFTBCripto.decryptDigitalSignature(request.getDigitalSignature().toByteArray(), publicKey);
        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, decriptedhash);

        if (request.getRawData().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }
        boolean answer = request.getRawData().getData().getReceiveAmountRequest().getAnswer();
        String receiverKey = request.getRawData().getData().getReceiveAmountRequest().getReceiverKey();
        String senderKey = request.getRawData().getData().getReceiveAmountRequest().getSenderKey();
        int transactionId = request.getRawData().getData().getReceiveAmountRequest().getTransactionId();

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

            Data sequencemessage = Data.newBuilder().setReceiveAmountResponse(logicResponse)
                    .setNonce(request.getRawData().getData().getNonce()).build();
            RawData unencriptedhash = RawData.newBuilder()
                    .setData(sequencemessage)
                    .setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded()))
                    .build();

            response = EncryptedStruck.newBuilder().setDigitalSignature(ByteString.copyFrom(BFTBCripto.digitalSign(
                BFTBCripto.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes()), _serverPrivateKey)))
                    .setRawData(unencriptedhash).build();

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

        byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64().encode(request.getRawData().getData().toByteArray()).getBytes());

        EncryptedStruck response;

        boolean isCorrect = Arrays.equals(calculatedHash, request.getDigitalSignature().toByteArray());

        if (request.getRawData().getSenderKey() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
            return;
        }

        if (!isCorrect) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(Label.ERROR_DECRYPT).asRuntimeException());
            return;
        }        

        try {
            AuditResponse auditResponse = AuditResponse.newBuilder().addAllSet(_bftb.audit(request.getRawData().getData().getAuditRequest().getKey().toStringUtf8())).build();
            
            Data sequencemessage = Data.newBuilder().setAuditResponse(auditResponse).setNonce(request.getRawData().getData().getNonce()).build();
            RawData unencriptedhash = RawData.newBuilder().setData(sequencemessage).setSenderKey(ByteString.copyFrom(_serverPublicKey.getEncoded())).build();

            response = EncryptedStruck.newBuilder().setDigitalSignature(ByteString.copyFrom(
                BFTBCripto.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes())))
                    .setRawData(unencriptedhash).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NonExistentAccount nea) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(nea.getMessage()).asRuntimeException());
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        } catch (BFTBDatabaseException bde) {
            responseObserver.onError(ABORTED.withDescription(bde.getMessage()).asRuntimeException());
        }

    }

    public void searchKeys(SearchKeysRequest request, StreamObserver<SearchKeysResponse> responseObserver) {

        SearchKeysResponse response = SearchKeysResponse.newBuilder().addAllResult(_bftb.getAllPublicKeys()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}