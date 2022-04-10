package pt.tecnico.bftb.client;

import java.security.PrivateKey;
import java.security.PublicKey;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.EncryptedMessage;
import pt.tecnico.bftb.grpc.Bftb.NonceResponse;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.grpc.BFTBGrpc;
import pt.tecnico.bftb.library.BFTBLibraryApp;
import pt.tecnico.bftb.library.ManipulatedPackageException;

public class BFTBFrontend {

    private int _port;
    private String _host;
    private BFTBLibraryApp _library;

    public BFTBFrontend(String host, int port, PrivateKey privateKey, PublicKey publickey) {
        _host = host;
        _port = port;
        _library = new BFTBLibraryApp(privateKey, publickey);
    }

    public BFTBGrpc.BFTBBlockingStub StubCreator() {
        final String target = _host + ":" + _port;
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        return BFTBGrpc.newBlockingStub(channel);
    }

    public OpenAccountResponse openAccount(ByteString encodedPublicKey) throws ManipulatedPackageException {
        System.out.println("im here in port" + _port);

        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        NonceResponse nonce = stub.getNonce(_library.getNonce(encodedPublicKey));
        return _library.openAccountResponse(stub.openAccount(_library.openAccount(encodedPublicKey, nonce.getNonce())));
    }

    public SendAmountResponse sendAmount(String senderPublicKey, String receiverPublicKey, int amount)
            throws ManipulatedPackageException {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        NonceResponse nonce = stub.getNonce(_library.getNonce(ByteString.copyFrom(senderPublicKey.getBytes())));

        return _library.sendAmountResponse(
                stub.sendAmount(_library.sendAmount(senderPublicKey, receiverPublicKey, amount, nonce.getNonce())));
    }

    public CheckAccountResponse checkAccount(String publicKey) throws ManipulatedPackageException {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        ByteString bytepublic = ByteString.copyFrom(publicKey.getBytes());
        NonceResponse nonce = stub.getNonce(_library.getNonce(bytepublic));
        return _library.checkAccountResponse(stub.checkAccount(_library.checkAccount(bytepublic, nonce.getNonce())));
    }

    public AuditResponse audit(String publicKey) throws ManipulatedPackageException {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        ByteString bytepublic = ByteString.copyFrom(publicKey.getBytes());
        NonceResponse nonce = stub.getNonce(_library.getNonce(bytepublic));
        return _library.auditResponse(stub.audit(_library.audit(bytepublic, nonce.getNonce())));
    }

    public ReceiveAmountResponse receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId,
            boolean accept) throws ManipulatedPackageException {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        NonceResponse nonce = stub.getNonce(_library.getNonce(ByteString.copyFrom(receiverPublicKey.getBytes())));
        return _library.receiveAmountResponse(
                stub.receiveAmount(_library.receiveAmount(receiverPublicKey, senderPublicKey, transactionId,
                        accept, nonce.getNonce())));
    }

    public SearchKeysResponse searchKeys() {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        return stub.searchKeys(_library.searchKeys());
    }
}
