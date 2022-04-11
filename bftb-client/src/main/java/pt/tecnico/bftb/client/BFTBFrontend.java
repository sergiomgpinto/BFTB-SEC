package pt.tecnico.bftb.client;

import java.security.PrivateKey;
import java.security.PublicKey;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import pt.tecnico.bftb.library.ManipulatedPackageException;

public class BFTBFrontend {

    private int _port;
    private String _host;
    private BFTBLibraryApp _library;
    final String _target;
    final ManagedChannel _channel;

    public BFTBFrontend(String host, int port, PrivateKey privateKey, PublicKey publickey) {
        _host = host;
        _port = port;
        _target = _host + ":" + _port;
        _channel = ManagedChannelBuilder.forTarget(_target).usePlaintext().build();
        _library = new BFTBLibraryApp(privateKey, publickey);
    }

    public BFTBGrpc.BFTBBlockingStub StubCreator() {
        return BFTBGrpc.newBlockingStub(_channel);
    }

    public OpenAccountResponse openAccount(ByteString encodedPublicKey) throws ManipulatedPackageException {

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        NonceResponse nonce = stub.getNonce(_library.getNonce(encodedPublicKey));

        EncryptedStruck encriptedRequest = _library.openAccount(encodedPublicKey, nonce.getNonce());

        EncryptedStruck encriptedResponse = stub.openAccount(encriptedRequest);

        return _library.openAccountResponse(encriptedResponse);
    }

    public SendAmountResponse sendAmount(String senderPublicKey, String receiverPublicKey, int amount)
            throws ManipulatedPackageException {

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        
        NonceResponse nonce = stub.getNonce(_library.getNonce(ByteString.copyFrom(senderPublicKey.getBytes())));

        EncryptedStruck encriptedRequest = _library.sendAmount(senderPublicKey, receiverPublicKey, amount, nonce.getNonce());

        EncryptedStruck encriptedResponse = stub.sendAmount(encriptedRequest);

        return _library.sendAmountResponse(encriptedResponse);
    }

    public CheckAccountResponse checkAccount(String publicKey) throws ManipulatedPackageException {

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        ByteString bytepublic = ByteString.copyFrom(publicKey.getBytes());

        NonceResponse nonce = stub.getNonce(_library.getNonce(bytepublic));

        EncryptedStruck encriptedRequest = _library.checkAccount(bytepublic, nonce.getNonce());

        EncryptedStruck encriptedResponse = stub.checkAccount(encriptedRequest);

        return _library.checkAccountResponse(encriptedResponse);
    }

    public AuditResponse audit(String publicKey) throws ManipulatedPackageException {

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        ByteString bytepublic = ByteString.copyFrom(publicKey.getBytes());

        NonceResponse nonce = stub.getNonce(_library.getNonce(bytepublic));

        EncryptedStruck encriptedRequest = _library.audit(bytepublic, nonce.getNonce());

        EncryptedStruck encriptedResponse = stub.audit(encriptedRequest);

        return _library.auditResponse(encriptedResponse);
    }

    public ReceiveAmountResponse receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId, boolean accept) throws ManipulatedPackageException{

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        NonceResponse nonce = stub.getNonce(_library.getNonce(ByteString.copyFrom(receiverPublicKey.getBytes())));


        EncryptedStruck encriptedRequest = _library.receiveAmount(receiverPublicKey, senderPublicKey, transactionId, accept, nonce.getNonce());
        
        EncryptedStruck encriptedResponse = stub.receiveAmount(encriptedRequest);

        return _library.receiveAmountResponse(encriptedResponse);
    }

    public SearchKeysResponse searchKeys() {
        
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();

        return stub.searchKeys(_library.searchKeys());
    }
}
