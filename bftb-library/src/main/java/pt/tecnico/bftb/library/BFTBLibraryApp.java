package pt.tecnico.bftb.library;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

import pt.tecnico.bftb.cripto.BFTBCripto;
import pt.tecnico.bftb.grpc.Bftb.AuditRequest;
import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.EncryptedStruck;
import pt.tecnico.bftb.grpc.Bftb.NonceRequest;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountRequest;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountRequest;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.RawData;
import pt.tecnico.bftb.grpc.Bftb.NonceResponse;

public class BFTBLibraryApp {

        PrivateKey _userPrivateKey;
        PublicKey _userPublicKey;
        int _nonce;

        public BFTBLibraryApp(PrivateKey privateKey, PublicKey publickey) {
                _userPrivateKey = privateKey;
                _userPublicKey = publickey;
        }

        /****************************
         * Protocol Messages
         ***********************************/
        // getNonce
        // The nonce interchange is a write operations therefore will be signed.

        public EncryptedStruck getNonce(ByteString encodedPublicKey) {

                NonceRequest request = NonceRequest
                                .newBuilder()
                                .setSenderKey(encodedPublicKey)
                                .build();

                RawData rawData = RawData.newBuilder()
                                .setNonceRequest(request)
                                .build();

                byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();

                ByteString digitalSignature = ByteString
                                .copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash(rawDataBytes), _userPrivateKey));

                return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();
        }

        public NonceResponse getNonceResponse(EncryptedStruck response)
                        throws ManipulatedPackageException, DetectedReplayAttackException {
                byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                                .encode(response.getRawData().toByteArray()).getBytes());

                PublicKey publicKey = null;

                try {
                        publicKey = KeyFactory.getInstance("RSA")
                                        .generatePublic(new X509EncodedKeySpec(response.getRawData().getNonceResponse()
                                                        .getServerPublicKey().toByteArray()));
                } catch (Exception e) {
                        System.out.println(e);
                }

                byte[] decryptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(),
                                publicKey);

                NonceResponse nonceResponse = response.getRawData().getNonceResponse();

                boolean isCorrect = Arrays.equals(calculatedHash, decryptedHash);

                if (!isCorrect) {
                        throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                                        " was sent.");
                }

                return NonceResponse.newBuilder()
                                .setNonce(nonceResponse.getNonce())
                                .build();
        }

        /****************************
         * Read Only Operations
         ***********************************/
        // checkAccount
        // audit
        // searchKeys

        public EncryptedStruck checkAccount(ByteString bytepublic, int nonce, String userPublicKeyString, int rid) {

                this._nonce = nonce;

                CheckAccountRequest checkAccountRequest = CheckAccountRequest.newBuilder()
                                .setKey(bytepublic)
                                .setUserKey(userPublicKeyString)
                                .setRid(rid)
                                .build();

                RawData rawData = RawData.newBuilder()
                                .setCheckAccountRequest(checkAccountRequest)
                                .setNonce(nonce)
                                .build();

                byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();
                ByteString digest = ByteString.copyFrom(BFTBCripto.hash(rawDataBytes));

                return EncryptedStruck.newBuilder()
                                .setDigest(digest)
                                .setRawData(rawData)
                                .build();
        }

        public CheckAccountResponse checkAccountResponse(EncryptedStruck response)
                        throws ManipulatedPackageException, DetectedReplayAttackException {
                byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                                .encode(response.getRawData().toByteArray()).getBytes());

                CheckAccountResponse accResponse = response.getRawData().getCheckAccountResponse();

                boolean isCorrect = Arrays.equals(calculatedHash, response.getDigest().toByteArray());

                if (!isCorrect) {
                        throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                                        " was sent.");
                }

                int receivedNonce = response.getRawData().getNonce();
                if (_nonce != receivedNonce) {
                        throw new DetectedReplayAttackException("Nonce verification failed in client app. " +
                                        "Message received doe snot hold property of freshness");
                }

                return CheckAccountResponse.newBuilder().setBalance(accResponse.getBalance())
                                .addAllPending(accResponse.getPendingList()).build();
        }

        public EncryptedStruck audit(ByteString bytepublic, int nonce, String userPublicKeyString, int rid) {

                this._nonce = nonce;

                AuditRequest auditRequest = AuditRequest.newBuilder().setKey(bytepublic)
                                .setUserKey(userPublicKeyString)
                                .setRid(rid)
                                .build();

                RawData rawData = RawData.newBuilder()
                                .setAuditRequest(auditRequest)
                                .setNonce(nonce)
                                .build();

                byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();
                ByteString digest = ByteString.copyFrom(BFTBCripto.hash(rawDataBytes));

                return EncryptedStruck.newBuilder()
                                .setDigest(digest)
                                .setRawData(rawData)
                                .build();
        }

        public AuditResponse auditResponse(EncryptedStruck response)
                        throws ManipulatedPackageException, DetectedReplayAttackException {

                byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                                .encode(response.getRawData().toByteArray()).getBytes());

                AuditResponse accResponse = response.getRawData().getAuditResponse();

                boolean isCorrect = Arrays.equals(calculatedHash, response.getDigest().toByteArray());

                if (!isCorrect) {
                        throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                                        " was sent.");
                }

                int receivedNonce = response.getRawData().getNonce();
                if (_nonce != receivedNonce) {
                        throw new DetectedReplayAttackException("Nonce verification failed in client app. " +
                                        "Message received doe snot hold property of freshness");
                }

                return AuditResponse.newBuilder().addAllSet(accResponse.getSetList()).build();
        }

        public EncryptedStruck searchKeys(int nonce, String userPublicKeyString, int rid) {

                this._nonce = nonce;

                SearchKeysRequest searchKeysRequest = SearchKeysRequest.newBuilder()
                                .setUserKey(userPublicKeyString)
                                .setRid(rid)
                                .build();

                RawData rawData = RawData.newBuilder()
                                .setSearchKeyRequest(searchKeysRequest)
                                .setNonce(nonce)
                                .build();

                byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();
                ByteString digest = ByteString.copyFrom(BFTBCripto.hash(rawDataBytes));

                return EncryptedStruck.newBuilder()
                                .setDigest(digest)
                                .setRawData(rawData)
                                .build();
        }

        public SearchKeysResponse searchKeysResponse(EncryptedStruck response)
                        throws ManipulatedPackageException, DetectedReplayAttackException {

                byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                                .encode(response.getRawData().toByteArray()).getBytes());

                SearchKeysResponse searchKeysResponse = response.getRawData().getSearchKeyResponse();

                boolean isCorrect = Arrays.equals(calculatedHash, response.getDigest().toByteArray());

                if (!isCorrect) {
                        throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                                        " was sent.");
                }

                int receivedNonce = response.getRawData().getNonce();
                if (_nonce != receivedNonce) {
                        throw new DetectedReplayAttackException("Nonce verification failed in client app. " +
                                        "Message received doe snot hold property of freshness");
                }

                return SearchKeysResponse.newBuilder().addAllResult(searchKeysResponse.getResultList()).build();
        }

        /****************************
         * Write Operations
         ***********************************/
        // openAccount
        // sendAmount
        // receiveAmount

        public EncryptedStruck openAccount(ByteString encodedPublicKey, int nonce, String username) {

                this._nonce = nonce;

                OpenAccountRequest openAccountRequest = OpenAccountRequest.newBuilder()
                                .setKey(encodedPublicKey)
                                .setUsername(username)
                                .build();

                RawData rawData = RawData.newBuilder()
                                .setOpenAccountRequest(openAccountRequest)
                                .setNonce(nonce)
                                .build();

                byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();

                ByteString digitalSignature = ByteString
                                .copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash(rawDataBytes), _userPrivateKey));

                return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();
        }

        public OpenAccountResponse openAccountResponse(EncryptedStruck response)
                        throws ManipulatedPackageException, DetectedReplayAttackException {

                byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                                .encode(response.getRawData().toByteArray()).getBytes());

                PublicKey publicKey = null;

                try {
                        publicKey = KeyFactory.getInstance("RSA")
                                        .generatePublic(new X509EncodedKeySpec(response.getRawData()
                                                        .getOpenAccountResponse().getServerPublicKey().toByteArray()));
                } catch (Exception e) {
                        System.out.println(e);
                }

                byte[] decryptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(),
                                publicKey);

                OpenAccountResponse accResponse = response.getRawData().getOpenAccountResponse();

                boolean isCorrect = Arrays.equals(calculatedHash, decryptedHash);

                if (!isCorrect) {
                        throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                                        " was sent.");
                }

                int receivedNonce = response.getRawData().getNonce();
                if (_nonce != receivedNonce) {
                        throw new DetectedReplayAttackException("Nonce verification failed in client app. " +
                                        "Message received doe snot hold property of freshness");
                }

                return OpenAccountResponse.newBuilder().setPublicKey(accResponse.getPublicKey())
                                .setResponse(accResponse.getResponse()).build();
        }

        public EncryptedStruck sendAmount(String senderPublicKey, String receiverPublicKey,
                        int amount, int nonce, int wts) {

                this._nonce = nonce;

                SendAmountRequest sendAmountRequest = SendAmountRequest.newBuilder()
                                .setSenderKey(senderPublicKey)
                                .setReceiverKey(receiverPublicKey)
                                .setAmount(amount)
                                .setWts(wts)
                                .build();

                RawData rawData = RawData.newBuilder()
                                .setSendAmountRequest(sendAmountRequest)
                                .setNonce(nonce)
                                .build();

                byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();

                ByteString digitalSignature = ByteString
                                .copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash(rawDataBytes), _userPrivateKey));

                return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();
        }

        public SendAmountResponse sendAmountResponse(EncryptedStruck response)
                        throws ManipulatedPackageException, DetectedReplayAttackException {

                byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                                .encode(response.getRawData().toByteArray()).getBytes());

                PublicKey publicKey = null;

                try {
                        publicKey = KeyFactory.getInstance("RSA")
                                        .generatePublic(new X509EncodedKeySpec(response.getRawData()
                                                        .getSendAmountResponse().getServerPublicKey().toByteArray())); // get
                                                                                                                       // the
                                                                                                                       // responses!!!
                } catch (Exception e) {
                        System.out.println(e);
                }

                byte[] decryptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(),
                                publicKey);

                SendAmountResponse accResponse = response.getRawData().getSendAmountResponse();

                boolean isCorrect = Arrays.equals(calculatedHash, decryptedHash);

                if (!isCorrect) {
                        throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                                        " was sent.");
                }

                int receivedNonce = response.getRawData().getNonce();
                if (_nonce != receivedNonce) {
                        throw new DetectedReplayAttackException("Nonce verification failed in client app. " +
                                        "Message received doe snot hold property of freshness");
                }

                return SendAmountResponse.newBuilder().setResponse(accResponse.getResponse())
                                .setResponse(accResponse.getResponse()).build();
        }

        public EncryptedStruck receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId,
                        boolean accept, int nonce, int wts) {

                this._nonce = nonce;

                ReceiveAmountRequest receiveAmountRequest = ReceiveAmountRequest.newBuilder()
                                .setReceiverKey(receiverPublicKey)
                                .setSenderKey(senderPublicKey)
                                .setTransactionId(transactionId)
                                .setAnswer(accept)
                                .setWts(wts)
                                .build();

                RawData rawData = RawData.newBuilder()
                                .setReceiveAmountRequest(receiveAmountRequest)
                                .setNonce(nonce)
                                .build();

                byte[] rawDataBytes = BaseEncoding.base64().encode(rawData.toByteArray()).getBytes();

                ByteString digitalSignature = ByteString
                                .copyFrom(BFTBCripto.digitalSign(BFTBCripto.hash(rawDataBytes), _userPrivateKey));

                return EncryptedStruck.newBuilder()
                                .setDigitalSignature(digitalSignature)
                                .setRawData(rawData)
                                .build();

        }

        public ReceiveAmountResponse receiveAmountResponse(EncryptedStruck response)
                        throws ManipulatedPackageException, DetectedReplayAttackException {
                byte[] calculatedHash = BFTBCripto.hash(BaseEncoding.base64()
                                .encode(response.getRawData().toByteArray()).getBytes());

                PublicKey publicKey = null;

                try {
                        publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(response
                                        .getRawData().getReceiveAmountResponse().getServerPublicKey().toByteArray()));
                } catch (Exception e) {
                        System.out.println(e);
                }

                byte[] decryptedHash = BFTBCripto.decryptDigitalSignature(response.getDigitalSignature().toByteArray(),
                                publicKey);

                ReceiveAmountResponse accResponse = response.getRawData().getReceiveAmountResponse();

                boolean isCorrect = Arrays.equals(calculatedHash, decryptedHash);

                if (!isCorrect) {
                        throw new ManipulatedPackageException("Either package was tempered or a older server response" +
                                        " was sent.");
                }

                int receivedNonce = response.getRawData().getNonce();
                if (_nonce != receivedNonce) {
                        throw new DetectedReplayAttackException("Nonce verification failed in client app. " +
                                        "Message received doe snot hold property of freshness");
                }

                return ReceiveAmountResponse.newBuilder().setResult(accResponse.getResult()).build();
        }
}
