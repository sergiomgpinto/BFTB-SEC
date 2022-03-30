package pt.tecnico.bftb.library;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import pt.tecnico.bftb.grpc.Bftb.OpenAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.AuditRequest;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.EncryptedMessage;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountRequest;

public class BFTBLibraryApp {

    PrivateKey _userPrivateKey;
    PublicKey _serverPublicKey = null;

    public BFTBLibraryApp(PrivateKey privateKey) {
        _userPrivateKey = privateKey;
        
    }


    public EncryptedMessage openAccount(ByteString encodedPublicKey) {
        OpenAccountRequest openaccountrequest = OpenAccountRequest.newBuilder().setKey(encodedPublicKey).build();
        String openAccountStoString = BaseEncoding.base64().encode(openaccountrequest.toByteArray());
        byte[] encryptedMessageBytes = null;
        try {
            Cipher encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, _userPrivateKey);
            byte[] secretMessageBytes = openAccountStoString.getBytes(StandardCharsets.UTF_8);
            encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {  //// ??
            e.printStackTrace();
        }
        
        String encodedMessage = new String(encryptedMessageBytes, StandardCharsets.UTF_8);
        return EncryptedMessage.newBuilder().setEncryptedmessage(encodedMessage).build();
    }

    public OpenAccountResponse openAccountResponse(EncryptedMessage response){
        
        byte[] decryptedMessageBytes = null;
        try {
            Cipher decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(Cipher.DECRYPT_MODE, _serverPublicKey); //server public Key

            decryptedMessageBytes = decryptCipher.doFinal(response.getEncryptedmessage().getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {  //// ??
            e.printStackTrace();
        }

        String decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);

        OpenAccountResponse responseOpen = null;
        try {
            responseOpen = OpenAccountResponse.parseFrom(BaseEncoding.base64().decode(decryptedMessage));
        } catch (InvalidProtocolBufferException e) { /// ??? 
            e.printStackTrace();
        }

        return responseOpen;
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
