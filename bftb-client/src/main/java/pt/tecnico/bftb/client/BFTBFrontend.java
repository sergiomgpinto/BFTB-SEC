package pt.tecnico.bftb.client;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.*;
import io.grpc.netty.shaded.io.netty.internal.tcnative.Library;
import io.opencensus.trace.Tracestate.Entry;

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
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

public class BFTBFrontend {

    private BFTBLibraryApp _library;
    String _target;
    ManagedChannel _channel;
    final int _numberOfAttemptsToRetransmitPacket = 5;
    final long _duration = 100;
    int ack = 0;
    int wts = 0;
    int rid = 0;
    ZKNaming zkNaming;
    ArrayList<ZKRecord> zkRecordsList;
    String mainPath = "/nodes";
    ArrayList<BFTBGrpc.BFTBBlockingStub> stubs;
    HashMap<String, Integer> responses;
    ArrayList<String> readList;

    public BFTBFrontend(String zoohost, String zooport, PrivateKey privateKey, PublicKey publickey) {
        _library = new BFTBLibraryApp(privateKey, publickey);
        zkNaming = new ZKNaming(zoohost, zooport);
    }

    public void StubCreator() {

        try {
            stubs = new ArrayList<>();
            zkRecordsList = new ArrayList<>(zkNaming.listRecords(mainPath));

            for (ZKRecord record : zkRecordsList) {
                _channel = ManagedChannelBuilder.forTarget(record.getURI()).usePlaintext().build();
                stubs.add(BFTBGrpc.newBlockingStub(_channel));
            }
        } catch (ZKNamingException e) {
            e.printStackTrace();
        }
    }

    public void shutdownChannels() {
        for (BFTBGrpc.BFTBBlockingStub stub : stubs) {
            ((ManagedChannel) stub.getChannel()).shutdown();
        }
    }

    public OpenAccountResponse openAccount(ByteString encodedPublicKey, String username)
            throws ManipulatedPackageException, DetectedReplayAttackException, ZooKeeperServer.MissingSessionException,
            PacketDropAttack, ResponseException {
        responses = new HashMap<>();
        EncryptedStruck encryptedResponse = null;
        StubCreator();
        OpenAccountResponse response = null;

        while (ack < 5) {
            for (BFTBGrpc.BFTBBlockingStub stub : stubs) {
                try {
                    System.out.println("stub qq coisa");
                    EncryptedStruck encryptedRequestNonce = _library.getNonce(encodedPublicKey);

                    EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);
                    System.out.println("nonce?");
                    NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

                    EncryptedStruck encryptedRequest = _library.openAccount(encodedPublicKey, nonce.getNonce(),
                            username);

                    int numberOfAttempts = 0;

                    do {
                        try {
                            System.out.println("hello");

                            encryptedResponse = stub
                                    .withDeadlineAfter((long) (_duration * Math.pow(2, numberOfAttempts)),
                                            TimeUnit.MILLISECONDS)
                                    .openAccount(encryptedRequest);
                            System.out.println("crash?");
                            response = _library.openAccountResponse(encryptedResponse);
                            String strResponse = BaseEncoding.base64().encode(response.toByteArray());
                            if (responses.computeIfPresent(strResponse, (k, v) -> v + 1) == null) {
                                responses.put(strResponse, 1);
                            }

                            ack++;
                            break;
                        } catch (io.grpc.StatusRuntimeException e) {
                            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                numberOfAttempts += 1;
                            } else {
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.
                        ack++;
                        System.out.println("para ti salgueiro");
                        if (responses.computeIfPresent(e.getMessage(), (k, v) -> v + 1) == null) {
                            responses.put(e.getMessage(), 1);
                        }
                        // // throw new StatusRuntimeException(Status.fromThrowable(e));
                    }
                }
            }
            if (ack < 5)
                ack = 0;
        }

        ack = 0;
        shutdownChannels();
        String key = Collections.max(responses.entrySet(), Map.Entry.comparingByValue()).getKey();

        response = null;
        try {
            System.out.println(responses);
            response = OpenAccountResponse.parseFrom(BaseEncoding.base64().decode(key));
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            throw new ResponseException(key);
        } // PROBABLY WILL NEED TO BE CHANGED TO BE COHERENT WITH
          // THE RESPONSE OF THE REPLICAS.
        return response;

    }

    public SendAmountResponse sendAmount(String senderPublicKey, String receiverPublicKey, int amount)
            throws ManipulatedPackageException, DetectedReplayAttackException, ZooKeeperServer.MissingSessionException,
            PacketDropAttack, ResponseException {
        responses = new HashMap<>();
        SendAmountResponse response = null;

        EncryptedStruck encryptedResponse = null;
        StubCreator();
        while (ack < 5) {
            for (BFTBGrpc.BFTBBlockingStub stub : stubs) {

                try {
                    EncryptedStruck encryptedRequestNonce = _library
                            .getNonce(ByteString.copyFrom(senderPublicKey.getBytes()));

                    EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

                    NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

                    EncryptedStruck encryptedRequest = _library.sendAmount(senderPublicKey, receiverPublicKey, amount,
                            nonce.getNonce(), wts);

                    int numberOfAttempts = 0;
                    do {
                        try {
                            encryptedResponse = stub
                                    .withDeadlineAfter((long) (_duration * Math.pow(2, numberOfAttempts)),
                                            TimeUnit.MILLISECONDS)
                                    .sendAmount(encryptedRequest);

                            wts++;
                            response = _library.sendAmountResponse(encryptedResponse);
                            String strResponse = BaseEncoding.base64().encode(response.toByteArray());

                            if (responses.computeIfPresent(strResponse, (k, v) -> v + 1) == null) {
                                responses.put(strResponse, 1);
                            }

                            ack += 1;
                            break;
                        } catch (io.grpc.StatusRuntimeException e) {
                            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                numberOfAttempts += 1;
                            } else {
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.

                        if (responses.computeIfPresent(e.getMessage(), (k, v) -> v + 1) == null) {
                            responses.put(e.getMessage(), 1);
                        }
                        // throw new StatusRuntimeException(Status.fromThrowable(e));
                    }
                }
            }

            if (ack < 5)
                ack = 0;
        }
        ack = 0;
        shutdownChannels();
        String key = Collections.max(responses.entrySet(), Map.Entry.comparingByValue()).getKey();

        response = null;
        try {
            System.out.println(responses);
            response = SendAmountResponse.parseFrom(BaseEncoding.base64().decode(key));
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            throw new ResponseException(key);
        } // PROBABLY WILL NEED TO BE CHANGED TO BE COHERENT WITH
          // THE RESPONSE OF THE REPLICAS.
        return response;

    }

    public CheckAccountResponse checkAccount(String dstPublicKey, String userPublicKey)
            throws ManipulatedPackageException, ZooKeeperServer.MissingSessionException, DetectedReplayAttackException,
            StatusRuntimeException, PacketDropAttack, ResponseException {

        readList = new ArrayList<>();
        CheckAccountResponse response = null;

        EncryptedStruck encryptedResponse = null;
        StubCreator();

        while (readList.size() < 5) {
            for (BFTBGrpc.BFTBBlockingStub stub : stubs) {

                try {

                    ByteString dstPublicBytes = ByteString.copyFrom(dstPublicKey.getBytes());
                    ByteString userPublicBytes = ByteString.copyFrom(userPublicKey.getBytes());

                    EncryptedStruck encryptedRequestNonce = _library.getNonce(userPublicBytes);

                    EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

                    NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

                    EncryptedStruck encriptedRequest = _library.checkAccount(dstPublicBytes, nonce.getNonce(),
                            userPublicKey);

                    int numberOfAttempts = 0;

                    do {
                        try {
                            encryptedResponse = stub
                                    .withDeadlineAfter((long) (_duration * Math.pow(2, numberOfAttempts)),
                                            TimeUnit.MILLISECONDS)
                                    .checkAccount(encriptedRequest);

                            rid++;
                            response = _library.checkAccountResponse(encryptedResponse);
                            String strResponse = BaseEncoding.base64().encode(response.toByteArray());

                            readList.add(strResponse);

                            break;
                        } catch (io.grpc.StatusRuntimeException e) {
                            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                numberOfAttempts += 1;
                            } else {
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.

                        readList.add(e.getMessage());

                    }
                }
            }
            if (readList.size() < 5) {
                readList = new ArrayList<>();
            }
        }
        shutdownChannels();

        response = null;

        String maxItem = null;
        int maxOcurrences = 0;
        for (String item : readList) {
            if (Collections.frequency(readList, item) >= maxOcurrences) {
                maxOcurrences = Collections.frequency(readList, item);
                maxItem = item;
            }
        }
        try {
            response = CheckAccountResponse.parseFrom(BaseEncoding.base64().decode(maxItem));
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            throw new ResponseException(maxItem);
        }

        CheckAccountResponse maxResponseWts = null;
        int maxWts = -1;
        for (String item : readList) {
            try {
                response = CheckAccountResponse.parseFrom(BaseEncoding.base64().decode(item));
                if (response.getWts() > maxWts) {
                    maxWts = response.getWts();
                    maxResponseWts = response;
                }
            } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            }
        }
        readList = new ArrayList<>();

        return maxResponseWts;

    }

    public AuditResponse audit(String dstPublicKey, String userPublicKey) throws ManipulatedPackageException,
            DetectedReplayAttackException, ZooKeeperServer.MissingSessionException, PacketDropAttack {
        EncryptedStruck encryptedResponse = null;
        StubCreator();

        while (ack < 5) {
            for (BFTBGrpc.BFTBBlockingStub stub : stubs) {

                try {
                    StubCreator();

                    ByteString dstPublicBytes = ByteString.copyFrom(dstPublicKey.getBytes());
                    ByteString userPublicBytes = ByteString.copyFrom(userPublicKey.getBytes());

                    EncryptedStruck encryptedRequestNonce = _library.getNonce(userPublicBytes);

                    EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

                    NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

                    EncryptedStruck encryptedRequest = _library.audit(dstPublicBytes, nonce.getNonce(), userPublicKey);

                    int numberOfAttempts = 0;

                    do {
                        try {
                            encryptedResponse = stub
                                    .withDeadlineAfter((long) (_duration * Math.pow(2, numberOfAttempts)),
                                            TimeUnit.MILLISECONDS)
                                    .audit(encryptedRequest);
                            ack += 1;
                            break;
                        } catch (io.grpc.StatusRuntimeException e) {
                            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                numberOfAttempts += 1;
                            } else {
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.
                        throw new StatusRuntimeException(Status.fromThrowable(e));
                    }
                }
            }
            if (ack < 5)
                ack = 0;
        }
        ack = 0;
        shutdownChannels();

        return _library.auditResponse(encryptedResponse);

    }

    public ReceiveAmountResponse receiveAmount(String receiverPublicKey, String senderPublicKey, int transactionId,
            boolean accept) throws ManipulatedPackageException, DetectedReplayAttackException,
            ZooKeeperServer.MissingSessionException, PacketDropAttack, ResponseException {

        responses = new HashMap<>();
        ReceiveAmountResponse response = null;

        EncryptedStruck encryptedResponse = null;
        StubCreator();

        while (ack < 5) {
            for (BFTBGrpc.BFTBBlockingStub stub : stubs) {

                try {
                    StubCreator();

                    EncryptedStruck encryptedRequestNonce = _library
                            .getNonce(ByteString.copyFrom(receiverPublicKey.getBytes()));

                    EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

                    NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

                    EncryptedStruck encryptedRequest = _library.receiveAmount(receiverPublicKey, senderPublicKey,
                            transactionId,
                            accept, nonce.getNonce(), wts);

                    int numberOfAttempts = 0;

                    do {
                        try {
                            encryptedResponse = stub
                                    .withDeadlineAfter((long) (_duration * Math.pow(2, numberOfAttempts)),
                                            TimeUnit.MILLISECONDS)
                                    .receiveAmount(encryptedRequest);

                            wts++;
                            response = _library.receiveAmountResponse(encryptedResponse);
                            String strResponse = BaseEncoding.base64().encode(response.toByteArray());

                            if (responses.computeIfPresent(strResponse, (k, v) -> v + 1) == null) {
                                responses.put(strResponse, 1);
                            }

                            ack += 1;
                            break;
                        } catch (io.grpc.StatusRuntimeException e) {
                            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                numberOfAttempts += 1;
                            } else {
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.
                        if (responses.computeIfPresent(e.getMessage(), (k, v) -> v + 1) == null) {
                            responses.put(e.getMessage(), 1);
                        }
                        // throw new StatusRuntimeException(Status.fromThrowable(e));
                    }
                }
            }
            if (ack < 5)
                ack = 0;
        }
        ack = 0;
        shutdownChannels();
        String key = Collections.max(responses.entrySet(), Map.Entry.comparingByValue()).getKey();

        response = null;
        try {
            System.out.println(responses);
            response = ReceiveAmountResponse.parseFrom(BaseEncoding.base64().decode(key));
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            throw new ResponseException(key);
        }
        return response;

    }

    public SearchKeysResponse searchKeys(String userPublicKeyString) throws ManipulatedPackageException,
            DetectedReplayAttackException, ZooKeeperServer.MissingSessionException, PacketDropAttack {
        BFTBGrpc.BFTBBlockingStub stub = null;

        try {
            StubCreator();

            EncryptedStruck encryptedRequestNonce = _library
                    .getNonce(ByteString.copyFrom(userPublicKeyString.getBytes()));

            EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

            NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

            EncryptedStruck encryptedRequest = _library.searchKeys(nonce.getNonce(), userPublicKeyString);

            int numberOfAttempts = 0;
            EncryptedStruck encryptedResponse = null;

            do {
                try {
                    encryptedResponse = stub
                            .withDeadlineAfter((long) (_duration * Math.pow(2, numberOfAttempts)),
                                    TimeUnit.MILLISECONDS)
                            .searchKeys(encryptedRequest);
                    break;
                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                        numberOfAttempts += 1;
                    } else {

                        throw new StatusRuntimeException(Status.fromThrowable(e));
                    }
                }
            } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

            if (numberOfAttempts == 5) {
                throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
            }

            return _library.searchKeysResponse(encryptedResponse);
        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in ServerImpl.
                throw new StatusRuntimeException(Status.fromThrowable(e));
            } else {// Error code for UNAVAILABLE: io exception
                    // Enters here when the replica the server was connected to crashes.
                throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
            }
        }
    }
}
