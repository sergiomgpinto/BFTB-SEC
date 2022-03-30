package pt.tecnico.bftb.client;

import java.security.PrivateKey;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.EncryptedMessage;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.grpc.BFTBGrpc;
import pt.tecnico.bftb.library.BFTBLibraryApp;

public class BFTBFrontend {

    private int _port;
    private String _host;
    private BFTBLibraryApp _library;

    public BFTBFrontend(String host, int port, PrivateKey privateKey) {
        _host = host;
        _port = port;
        _library = new BFTBLibraryApp(privateKey);
    }

    public BFTBGrpc.BFTBBlockingStub StubCreator() {
        final String target = _host + ":" + _port;
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        return BFTBGrpc.newBlockingStub(channel);
    }

    public OpenAccountResponse openAccount(ByteString encodedPublicKey) {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        return _library.openAccountResponse(stub.openAccount(_library.openAccount(encodedPublicKey)));
    }

    public SendAmountResponse sendAmount(String senderPublicKey, String receiverPublicKey,
                                         int amount) {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        return stub.sendAmount(_library.sendAmount(senderPublicKey, receiverPublicKey, amount));
    }

    public CheckAccountResponse checkAccount(String publicKey) {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        return stub.checkAccount(_library.checkAccount(publicKey));
    }

    public AuditResponse audit(String publicKey) {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        return stub.audit(_library.audit(publicKey));
    }

    public ReceiveAmountResponse receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId, boolean accept) {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        return stub.receiveAmount(_library.receiveAmount(receiverPublicKey,senderPublicKey,transactionId,accept));
    }

    public SearchKeysResponse searchKeys() {
        BFTBGrpc.BFTBBlockingStub stub = StubCreator();
        return stub.searchKeys(_library.searchKeys());
    }
}
