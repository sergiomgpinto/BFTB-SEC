package pt.tecnico.bftb.library;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.Provider.Service;
import java.util.TreeSet;

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
import pt.tecnico.bftb.grpc.Bftb.EncryptedStruck;
import pt.tecnico.bftb.grpc.Bftb.NonceRequest;
import pt.tecnico.bftb.grpc.Bftb.NonceResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.Sequencemessage;
import pt.tecnico.bftb.grpc.Bftb.Unencriptedhash;

public class BFTBLibraryApp {

    PrivateKey _userPrivateKey;
    PublicKey _userPublicKey;
    PublicKey _serverPublicKey = null;

    public BFTBLibraryApp(PrivateKey privateKey, PublicKey publickey) {
        _userPrivateKey = privateKey;
        _userPublicKey = publickey;
    }

    public NonceRequest getNonce(ByteString encodedPublicKey) {
        return NonceRequest.newBuilder().setSenderKey(encodedPublicKey).build();
    }

    private byte[] hash(String inputdata){
        byte[] data = inputdata.getBytes();

        // hash

        byte[] hash = null;
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
            sha.update(data);
            hash = sha.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return hash;

    }

    private String digitalsign (byte[] inputhash, PrivateKey signprivatekey){

        // Signature dsaForSign;
        // byte[] signature = null;
        // try {
        //     dsaForSign = Signature.getInstance("SHA256withRSA");
        //     dsaForSign.initSign(signprivatekey);
        //     dsaForSign.update(inputhash);
        //     signature = dsaForSign.sign();
        // } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
        //     e.printStackTrace();
        // }

        Cipher cipher;
        byte[] signature = null;
        TreeSet<String> algorithms = new TreeSet<>();
            for (Provider provider : Security.getProviders())
                for (Service service : provider.getServices())
                    if (service.getType().equals("Signature"))
                        algorithms.add(service.getAlgorithm());
            for (String algorithm : algorithms)
                System.out.println(algorithm);
        try {
            cipher = Cipher.getInstance("SHA256withRSA");
            cipher.init(Cipher.ENCRYPT_MODE, signprivatekey);
            signature = cipher.doFinal(inputhash);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        return new String(signature, StandardCharsets.UTF_8);

    }

    public EncryptedStruck openAccount(ByteString encodedPublicKey, int nonce) {
        Sequencemessage sequencemessage = Sequencemessage.newBuilder().setMessage("value").setNonce(nonce).build(); ///change value
        Unencriptedhash unencriptedhash = Unencriptedhash.newBuilder().setSequencemessage(sequencemessage).setSenderKey(encodedPublicKey).build();

        String sequencemessagetoencrypt = BaseEncoding.base64().encode(sequencemessage.toByteArray());
        
        return EncryptedStruck.newBuilder().setEncryptedhash(digitalsign(hash(sequencemessagetoencrypt), _userPrivateKey)).setUnencriptedhash(unencriptedhash).build();
    }

    public OpenAccountResponse openAccountResponse(EncryptedStruck response){
        
        return null;
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
