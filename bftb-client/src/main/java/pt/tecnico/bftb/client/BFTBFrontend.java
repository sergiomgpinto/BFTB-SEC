package pt.tecnico.bftb.client;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.zookeeper.server.ZooKeeperServer;
import pt.tecnico.bftb.grpc.BFTBGrpc;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.EncryptedStruck;
import pt.tecnico.bftb.grpc.Bftb.NonceResponse;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.library.BFTBLibraryApp;
import pt.tecnico.bftb.library.DetectedReplayAttackException;
import pt.tecnico.bftb.library.ManipulatedPackageException;

public class BFTBFrontend {

    private int _port;
    private String _host;
    private BFTBLibraryApp _library;
    String _target;
    ManagedChannel _channel;

    public BFTBFrontend(String host, int port, PrivateKey privateKey, PublicKey publickey) {
        _host = host;
        _port = port;
        _target = _host + ":" + _port;
        _channel = ManagedChannelBuilder.forTarget(_target).usePlaintext().build();
        _library = new BFTBLibraryApp(privateKey, publickey);
    }

    public void setNewTarget(String host, int port) {
        _target = host + ":" + port;
        _channel = ManagedChannelBuilder.forTarget(_target).usePlaintext().build();
    }

    public BFTBGrpc.BFTBBlockingStub StubCreator() {

        return BFTBGrpc.newBlockingStub(_channel);
    }

    public OpenAccountResponse openAccount(ByteString encodedPublicKey) throws ManipulatedPackageException, DetectedReplayAttackException {

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        NonceResponse nonce = stub.getNonce(_library.getNonce(encodedPublicKey));

        EncryptedStruck encryptedRequest = _library.openAccount(encodedPublicKey, nonce.getNonce());

        EncryptedStruck encryptedResponse = stub.openAccount(encryptedRequest);

        return _library.openAccountResponse(encryptedResponse);
    }

    public SendAmountResponse sendAmount(String senderPublicKey, String receiverPublicKey, int amount)
            throws ManipulatedPackageException, DetectedReplayAttackException{

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        
        NonceResponse nonce = stub.getNonce(_library.getNonce(ByteString.copyFrom(senderPublicKey.getBytes())));

        EncryptedStruck encryptedRequest = _library.sendAmount(senderPublicKey, receiverPublicKey, amount, nonce.getNonce());

        EncryptedStruck encryptedResponse = stub.sendAmount(encryptedRequest);

        return _library.sendAmountResponse(encryptedResponse);
    }

    public CheckAccountResponse checkAccount(String dstPublicKey,String userPublicKey) throws ManipulatedPackageException
            ,ZooKeeperServer.MissingSessionException, DetectedReplayAttackException{

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        ByteString dstPublicBytes = ByteString.copyFrom(dstPublicKey.getBytes());
        ByteString userPublicBytes = ByteString.copyFrom(userPublicKey.getBytes());

        NonceResponse nonce = stub.getNonce(_library.getNonce(userPublicBytes));

        EncryptedStruck encriptedRequest = _library.checkAccount(dstPublicBytes, nonce.getNonce(),userPublicKey);

        EncryptedStruck encriptedResponse = null;
        //try {
        encriptedResponse = stub.checkAccount(encriptedRequest);
        //}
        /*catch (StatusRuntimeException sre) {
            if (sre.getStatus().equals(Status.UNAVAILABLE) || sre.getMessage().equals("io exception")) {
                throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
            }
            else if (!sre.getStatus().equals(Status.UNAVAILABLE)) {
                throw new StatusRuntimeException(sre.getStatus());
            }

        }*/
        return _library.checkAccountResponse(encriptedResponse);
    }

    public AuditResponse audit(String dstPublicKey,String userPublicKey) throws ManipulatedPackageException, DetectedReplayAttackException {

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        ByteString dstPublicBytes = ByteString.copyFrom(dstPublicKey.getBytes());
        ByteString userPublicBytes = ByteString.copyFrom(userPublicKey.getBytes());

        NonceResponse nonce = stub.getNonce(_library.getNonce(userPublicBytes));

        EncryptedStruck encryptedRequest = _library.audit(dstPublicBytes, nonce.getNonce(),userPublicKey);

        EncryptedStruck encryptedResponse = stub.audit(encryptedRequest);

        return _library.auditResponse(encryptedResponse);
    }

    public ReceiveAmountResponse receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId, boolean accept) throws ManipulatedPackageException, DetectedReplayAttackException{

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        NonceResponse nonce = stub.getNonce(_library.getNonce(ByteString.copyFrom(receiverPublicKey.getBytes())));


        EncryptedStruck encryptedRequest = _library.receiveAmount(receiverPublicKey, senderPublicKey, transactionId, accept, nonce.getNonce());

        EncryptedStruck encryptedResponse = stub.receiveAmount(encryptedRequest);

        return _library.receiveAmountResponse(encryptedResponse);
    }

    public SearchKeysResponse searchKeys(String userPublicKeyString) throws ManipulatedPackageException, DetectedReplayAttackException {
        
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        NonceResponse nonce = stub.getNonce(_library.getNonce(ByteString.copyFrom(userPublicKeyString.getBytes())));

        EncryptedStruck encryptedRequest = _library.searchKeys(nonce.getNonce(),userPublicKeyString);

        EncryptedStruck encryptedResponse = stub.searchKeys(encryptedRequest);

        return _library.searchKeysResponse(encryptedResponse);
    }
}
