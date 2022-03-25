package pt.tecnico.bftb.library;

import com.google.protobuf.ByteString;

import pt.tecnico.bftb.grpc.Bftb.OpenAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.AuditRequest;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountRequest;

public class BFTBLibraryApp {

    public OpenAccountRequest openAccount(ByteString encodedPublicKey) {
        return OpenAccountRequest.newBuilder().setKey(encodedPublicKey).build();
    }

    public SendAmountRequest sendAmount(String senderPublicKey, String receiverPublicKey,
                                        int amount) {
        return SendAmountRequest.newBuilder().setSenderKey(senderPublicKey)
                .setReceiverKey(receiverPublicKey).setAmount(amount)
                .build();
    }

    public CheckAccountRequest checkAccount(String publicKey) {
        return CheckAccountRequest.newBuilder().setKey(publicKey).build();
    }

    public AuditRequest audit(String publicKey) {
        return AuditRequest.newBuilder().setKey(publicKey).build();
    }

    public ReceiveAmountRequest receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId, boolean accept) {
        return ReceiveAmountRequest.newBuilder().setReceiverKey(receiverPublicKey).setSenderKey(senderPublicKey)
                .setTransactionId(transactionId).setAnswer(accept).build();
    }

    public SearchKeysRequest searchKeys() {
        return SearchKeysRequest.newBuilder().build();
    }
}
