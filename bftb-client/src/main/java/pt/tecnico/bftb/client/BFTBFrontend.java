package pt.tecnico.bftb.client;

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
        _channel.shutdown();
        _channel = ManagedChannelBuilder.forTarget(_target).usePlaintext().build();
    }

    public BFTBGrpc.BFTBBlockingStub StubCreator() {

        return BFTBGrpc.newBlockingStub(_channel);
    }

    public OpenAccountResponse openAccount(ByteString encodedPublicKey) throws ManipulatedPackageException, DetectedReplayAttackException, ZooKeeperServer.MissingSessionException {

        try {
            BFTBGrpc.BFTBBlockingStub stub = StubCreator();

            EncryptedStruck encryptedRequestNonce = _library.getNonce(encodedPublicKey);

            EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

            NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

            EncryptedStruck encryptedRequest = _library.openAccount(encodedPublicKey, nonce.getNonce());

            EncryptedStruck encryptedResponse = stub.openAccount(encryptedRequest);

            return _library.openAccountResponse(encryptedResponse);
        }
        catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode().value() != 14){ // Enters here for every other exception thrown in ServerImpl.
                throw new StatusRuntimeException(Status.fromThrowable(e));
            }
            else {// Error code for UNAVAILABLE: io exception
                // Enters here when the replica the server was connected to crashes.
                throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
            }
        }
    }

    public SendAmountResponse sendAmount(String senderPublicKey, String receiverPublicKey, int amount)
            throws ManipulatedPackageException, DetectedReplayAttackException, ZooKeeperServer.MissingSessionException {
        
        try {
            BFTBGrpc.BFTBBlockingStub stub = StubCreator();

            EncryptedStruck encryptedRequestNonce = _library.getNonce(ByteString.copyFrom(senderPublicKey.getBytes()));

            EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

            NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

            EncryptedStruck encryptedRequest = _library.sendAmount(senderPublicKey, receiverPublicKey, amount, nonce.getNonce());

            EncryptedStruck encryptedResponse = stub.sendAmount(encryptedRequest);

            return _library.sendAmountResponse(encryptedResponse);
        }
        catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode().value() != 14){ // Enters here for every other exception thrown in ServerImpl.
                throw new StatusRuntimeException(Status.fromThrowable(e));
            }
            else {// Error code for UNAVAILABLE: io exception
                // Enters here when the replica the server was connected to crashes.
                throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
            }
        }
    }

    public CheckAccountResponse checkAccount(String dstPublicKey,String userPublicKey) throws ManipulatedPackageException
            , ZooKeeperServer.MissingSessionException, DetectedReplayAttackException, StatusRuntimeException {

        try {
            BFTBGrpc.BFTBBlockingStub stub = StubCreator();

            ByteString dstPublicBytes = ByteString.copyFrom(dstPublicKey.getBytes());
            ByteString userPublicBytes = ByteString.copyFrom(userPublicKey.getBytes());

            EncryptedStruck encryptedRequestNonce = _library.getNonce(userPublicBytes);

            EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

            NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

            EncryptedStruck encriptedRequest = _library.checkAccount(dstPublicBytes, nonce.getNonce(),userPublicKey);

            EncryptedStruck encriptedResponse = null;

            encriptedResponse = stub.checkAccount(encriptedRequest);

            return _library.checkAccountResponse(encriptedResponse);
        }
        catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode().value() != 14){ // Enters here for every other exception thrown in ServerImpl.
                throw new StatusRuntimeException(Status.fromThrowable(e));
            }
            else {// Error code for UNAVAILABLE: io exception
                // Enters here when the replica the server was connected to crashes.
                throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
            }
        }
    }

    public AuditResponse audit(String dstPublicKey,String userPublicKey) throws ManipulatedPackageException, DetectedReplayAttackException, ZooKeeperServer.MissingSessionException {

        try {
            BFTBGrpc.BFTBBlockingStub stub = StubCreator();

            ByteString dstPublicBytes = ByteString.copyFrom(dstPublicKey.getBytes());
            ByteString userPublicBytes = ByteString.copyFrom(userPublicKey.getBytes());

            EncryptedStruck encryptedRequestNonce = _library.getNonce(userPublicBytes);

            EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

            NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

            EncryptedStruck encryptedRequest = _library.audit(dstPublicBytes, nonce.getNonce(), userPublicKey);

            EncryptedStruck encryptedResponse = stub.audit(encryptedRequest);

            return _library.auditResponse(encryptedResponse);
        }
        catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode().value() != 14){ // Enters here for every other exception thrown in ServerImpl.
                throw new StatusRuntimeException(Status.fromThrowable(e));
            }
            else {// Error code for UNAVAILABLE: io exception
                // Enters here when the replica the server was connected to crashes.
                throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
            }
        }
    }

    public ReceiveAmountResponse receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId, boolean accept) throws ManipulatedPackageException, DetectedReplayAttackException, ZooKeeperServer.MissingSessionException {

        try {
            BFTBGrpc.BFTBBlockingStub stub = StubCreator();

            EncryptedStruck encryptedRequestNonce = _library.getNonce(ByteString.copyFrom(receiverPublicKey.getBytes()));

            EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

            NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

            EncryptedStruck encryptedRequest = _library.receiveAmount(receiverPublicKey, senderPublicKey, transactionId, accept, nonce.getNonce());

            EncryptedStruck encryptedResponse = stub.receiveAmount(encryptedRequest);

            return _library.receiveAmountResponse(encryptedResponse);
        }
        catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode().value() != 14){ // Enters here for every other exception thrown in ServerImpl.
                throw new StatusRuntimeException(Status.fromThrowable(e));
            }
            else {// Error code for UNAVAILABLE: io exception
                // Enters here when the replica the server was connected to crashes.
                throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
            }
        }
    }

    public SearchKeysResponse searchKeys(String userPublicKeyString) throws ManipulatedPackageException, DetectedReplayAttackException, ZooKeeperServer.MissingSessionException {
        
        try {
            BFTBGrpc.BFTBBlockingStub stub = StubCreator();

            EncryptedStruck encryptedRequestNonce = _library.getNonce(ByteString.copyFrom(userPublicKeyString.getBytes()));

            EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

            NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

            EncryptedStruck encryptedRequest = _library.searchKeys(nonce.getNonce(), userPublicKeyString);

            EncryptedStruck encryptedResponse = stub.searchKeys(encryptedRequest);

            return _library.searchKeysResponse(encryptedResponse);
        }
        catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode().value() != 14){ // Enters here for every other exception thrown in ServerImpl.
                throw new StatusRuntimeException(Status.fromThrowable(e));
            }
            else {// Error code for UNAVAILABLE: io exception
                // Enters here when the replica the server was connected to crashes.
                throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
            }
        }
    }
}
