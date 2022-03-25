package pt.tecnico.bftb.server;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;
import pt.tecnico.bftb.server.domain.Label;
import pt.tecnico.bftb.server.domain.NoAccountException;
import pt.tecnico.bftb.server.domain.exception.NoAuthorization;
import pt.tecnico.bftb.server.domain.exception.NonExistentAccount;
import pt.tecnico.bftb.grpc.BFTBGrpc;
import pt.tecnico.bftb.server.domain.BFTBServerLogic;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.AuditRequest;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.SendAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.server.domain.exception.NonExistentTransaction;

import java.util.ArrayList;
import java.util.List;

import static io.grpc.Status.*;

public class BFTBImpl extends BFTBGrpc.BFTBImplBase {

    private BFTBServerLogic _bftb = new BFTBServerLogic();

    @Override
    public void openAccount(OpenAccountRequest request, StreamObserver<OpenAccountResponse> responseObserver) {
        ByteString key = request.getKey();
        OpenAccountResponse response;

        if (key == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
        }
        try {
            String ret = _bftb.openAccount(key);

            if (ret.indexOf(":") != -1){
                String[] values = ret.split(":");
                response = OpenAccountResponse.newBuilder().setResponse(values[0]).setPublicKey(values[1]).build();
            }
            else{
                response = OpenAccountResponse.newBuilder().setResponse(ret).build();
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        }

    }
    @Override
    public void sendAmount(SendAmountRequest request, StreamObserver<SendAmountResponse> responseObserver) {
        String senderKey = request.getSenderKey();
        String receiverKey = request.getReceiverKey();

        int amount = request.getAmount();

        SendAmountResponse response;

        if (senderKey == null || senderKey.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
        }
        if (receiverKey == null || receiverKey.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
        }
        if (senderKey.equals(receiverKey)) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_ARGS_SEND_AMOUNT).asRuntimeException());
        }
        if (amount <= 0){
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_AMOUNT).asRuntimeException());
        }

        try {
            response = SendAmountResponse.newBuilder().setResponse(_bftb.sendAmount(senderKey, receiverKey, amount))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        } catch (NoAccountException nae) {
            responseObserver.onError(ABORTED.withDescription(nae.getMessage()).asRuntimeException());
        }
    }
    @Override
    public void checkAccount(CheckAccountRequest request, StreamObserver<CheckAccountResponse> responseObserver) {
        String key = request.getKey();
        CheckAccountResponse response;

        try {

            List<String> ret = _bftb.checkAccount(key);

            //Owner of the account has no pending transactions.
            if (ret.size() == 1){
                List<String> pending = new ArrayList<>();
                pending.add(Label.NO_PENDING_TRANSACTIONS);
                response = CheckAccountResponse.newBuilder().setBalance(Integer.parseInt(ret.get(0)))
                        .addAllPending(pending).build();
            }
            else{
                response = CheckAccountResponse.newBuilder().setBalance(Integer.parseInt(ret.get(0)))
                        .addAllPending(ret.subList(1, ret.size())).build();
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NonExistentAccount nea) {
            responseObserver.onError(ABORTED.withDescription(nea.getMessage()).asRuntimeException());
        }

    }

    @Override
    public void receiveAmount(ReceiveAmountRequest request, StreamObserver<ReceiveAmountResponse> responseObserver) {
        String receiverKey = request.getReceiverKey();
        String senderKey = request.getSenderKey();
        int transactionId = request.getTransactionId();
        boolean answer = request.getAnswer();

        if (receiverKey == null || receiverKey.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
        }
        if (senderKey == null || senderKey.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
        }
        if (senderKey.equals(receiverKey)) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_ARGS_SEND_AMOUNT).asRuntimeException());
        }
        if (transactionId <= 0) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_TRANSACTION_ID).asRuntimeException());
        }

        try{
            ReceiveAmountResponse response = ReceiveAmountResponse.newBuilder().setResult(
                    _bftb.receiveAmount(receiverKey,senderKey,transactionId,answer)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (NonExistentAccount nea) {
            responseObserver.onError(ABORTED.withDescription(nea.getMessage()).asRuntimeException());
        }
        catch (NonExistentTransaction net) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(net.getMessage()).asRuntimeException());
        }
        catch (NoAuthorization na) {
            responseObserver.onError(PERMISSION_DENIED.withDescription(na.getMessage()).asRuntimeException());
        }

    }

    @Override
    public void audit(AuditRequest request, StreamObserver<AuditResponse> responseObserver) {
        String key = request.getKey();

        if (key == null || key.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(Label.INVALID_PUBLIC_KEY).asRuntimeException());
        }

        try {
            AuditResponse response = AuditResponse.newBuilder().addAllSet(_bftb.audit(key)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NonExistentAccount nea) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(nea.getMessage()).asRuntimeException());
        }

        catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            responseObserver.onError(UNKNOWN.withDescription(Label.UNKNOWN_ERROR).asRuntimeException());
        }

    }
    public void searchKeys(SearchKeysRequest request, StreamObserver<SearchKeysResponse> responseObserver) {

        SearchKeysResponse response = SearchKeysResponse.newBuilder().addAllResult(_bftb.getAllPublicKeys()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}