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
import pt.tecnico.bftb.cripto.BFTBCriptoApp;
import pt.tecnico.bftb.grpc.BFTBGrpc;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.EncryptedStruck;
import pt.tecnico.bftb.grpc.Bftb.NonceRequest;
import pt.tecnico.bftb.grpc.Bftb.NonceResponse;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.Sequencemessage;
import pt.tecnico.bftb.grpc.Bftb.Unencriptedhash;
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

        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64()
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

        byte[] decriptedhash = BFTBCriptoApp.decrypt(request.getEncryptedhash().toByteArray(), publicKey);
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

            response = EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(BFTBCriptoApp.digitalsign(
                BFTBCriptoApp.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes()), _serverPrivateKey)))
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

        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64()
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

        byte[] decriptedhash = BFTBCriptoApp.decrypt(request.getEncryptedhash().toByteArray(), senderPubKey);
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

            response = EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(BFTBCriptoApp.digitalsign(
                BFTBCriptoApp.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes()), _serverPrivateKey)))
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

        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64()
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
                BFTBCriptoApp.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes())))
                    .setUnencriptedhash(unencriptedhash).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NonExistentAccount nea) {
            responseObserver.onError(ABORTED.withDescription(nea.getMessage()).asRuntimeException());
        }

    }

    @Override
    public void receiveAmount(EncryptedStruck request, StreamObserver<EncryptedStruck> responseObserver) {

        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64()
                .encode(request.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

        PublicKey publicKey = _bftb.searchAccount(request.getUnencriptedhash()
                .getSequencemessage().getReceiveAmountRequest().getReceiverKey()).getPublicKey();

        byte[] decriptedhash = BFTBCriptoApp.decrypt(request.getEncryptedhash().toByteArray(), publicKey);
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

            response = EncryptedStruck.newBuilder().setEncryptedhash(ByteString.copyFrom(BFTBCriptoApp.digitalsign(
                BFTBCriptoApp.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes()), _serverPrivateKey)))
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

        byte[] calculatedHash = BFTBCriptoApp.hash(BaseEncoding.base64().encode(request.getUnencriptedhash().getSequencemessage().toByteArray()).getBytes());

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
                BFTBCriptoApp.hash(BaseEncoding.base64().encode(sequencemessage.toByteArray()).getBytes())))
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

}