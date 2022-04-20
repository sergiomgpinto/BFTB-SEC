package pt.tecnico.bftb.client;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;

import io.grpc.*;
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
    ZKNaming zkNaming;
    ArrayList<ZKRecord> zkRecordsList;
    String mainPath = "/nodes";
    ArrayList<BFTBGrpc.BFTBBlockingStub> stubs;

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
            PacketDropAttack {

        EncryptedStruck encryptedResponse = null;
        StubCreator();

        while (ack < 5) {
            for (BFTBGrpc.BFTBBlockingStub stub : stubs) {
                try {

                    EncryptedStruck encryptedRequestNonce = _library.getNonce(encodedPublicKey);

                    EncryptedStruck encryptedResponseNonce = stub.getNonce(encryptedRequestNonce);

                    NonceResponse nonce = _library.getNonceResponse(encryptedResponseNonce);

                    EncryptedStruck encryptedRequest = _library.openAccount(encodedPublicKey, nonce.getNonce(),
                            username);

                    int numberOfAttempts = 0;

                    do {
                        try {
                            encryptedResponse = stub
                                    .withDeadlineAfter((long) (_duration * Math.pow(2, numberOfAttempts)),
                                            TimeUnit.MILLISECONDS)
                                    .openAccount(encryptedRequest);
                            ack += 1;
                            break;
                        } catch (io.grpc.StatusRuntimeException e) {
                            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                numberOfAttempts += 1;
                            } else {
                                shutdownChannels();
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        shutdownChannels();
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.
                        shutdownChannels();
                        throw new StatusRuntimeException(Status.fromThrowable(e));
                    } else {// Error code for UNAVAILABLE: io exception
                            // Enters here when the replica the server was connected to crashes.
                        shutdownChannels();
                        throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
                    }
                }
            }
            if (ack < 5)
                ack = 0;
        }
        ack = 0;
        shutdownChannels();

        return _library.openAccountResponse(encryptedResponse); // PROBABLY WILL NEED TO BE CHANGED TO BE COHERENT WITH
                                                                // THE RESPONSE OF THE REPLICAS.

    }

    public SendAmountResponse sendAmount(String senderPublicKey, String receiverPublicKey, int amount)
            throws ManipulatedPackageException, DetectedReplayAttackException, ZooKeeperServer.MissingSessionException,
            PacketDropAttack {

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
                            nonce.getNonce());

                    int numberOfAttempts = 0;
                    do {
                        try {
                            encryptedResponse = stub
                                    .withDeadlineAfter((long) (_duration * Math.pow(2, numberOfAttempts)),
                                            TimeUnit.MILLISECONDS)
                                    .sendAmount(encryptedRequest);
                            ack += 1;
                            break;
                        } catch (io.grpc.StatusRuntimeException e) {
                            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                numberOfAttempts += 1;
                            } else {
                                shutdownChannels();
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        shutdownChannels();
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.
                        shutdownChannels();
                        throw new StatusRuntimeException(Status.fromThrowable(e));
                    } else {// Error code for UNAVAILABLE: io exception
                            // Enters here when the replica the server was connected to crashes.
                        shutdownChannels();
                        throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
                    }
                }
            }

            if (ack < 5)
                ack = 0;
        }
        ack = 0;
        shutdownChannels();

        return _library.sendAmountResponse(encryptedResponse);

    }

    public CheckAccountResponse checkAccount(String dstPublicKey, String userPublicKey)
            throws ManipulatedPackageException, ZooKeeperServer.MissingSessionException, DetectedReplayAttackException,
            StatusRuntimeException, PacketDropAttack {

        EncryptedStruck encryptedResponse = null;
        StubCreator();

        while (ack < 5) {
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
                            ack += 1;

                            break;
                        } catch (io.grpc.StatusRuntimeException e) {
                            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                numberOfAttempts += 1;
                            } else {
                                shutdownChannels();
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        shutdownChannels();
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.
                        shutdownChannels();
                        throw new StatusRuntimeException(Status.fromThrowable(e));
                    } else {// Error code for UNAVAILABLE: io exception
                            // Enters here when the replica the server was connected to crashes.
                        shutdownChannels();
                        throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
                    }
                }
            }
            if (ack < 5)
                ack = 0;
        }
        ack = 0;
        shutdownChannels();

        return _library.checkAccountResponse(encryptedResponse);

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
                                shutdownChannels();
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        shutdownChannels();
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.
                        shutdownChannels();
                        throw new StatusRuntimeException(Status.fromThrowable(e));
                    } else {// Error code for UNAVAILABLE: io exception
                            // Enters here when the replica the server was connected to crashes.
                        shutdownChannels();
                        throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
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
            ZooKeeperServer.MissingSessionException, PacketDropAttack {
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
                            accept, nonce.getNonce());

                    int numberOfAttempts = 0;

                    do {
                        try {
                            encryptedResponse = stub
                                    .withDeadlineAfter((long) (_duration * Math.pow(2, numberOfAttempts)),
                                            TimeUnit.MILLISECONDS)
                                    .receiveAmount(encryptedRequest);
                            ack += 1;
                            break;
                        } catch (io.grpc.StatusRuntimeException e) {
                            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                numberOfAttempts += 1;
                            } else {
                                shutdownChannels();
                                throw new StatusRuntimeException(Status.fromThrowable(e));
                            }
                        }
                    } while (_numberOfAttemptsToRetransmitPacket - numberOfAttempts > 0);

                    if (numberOfAttempts == 5) {
                        shutdownChannels();
                        throw new PacketDropAttack(Label.PACKET_DROP_ATTACK);
                    }

                } catch (io.grpc.StatusRuntimeException e) {
                    if (e.getStatus().getCode().value() != 14) { // Enters here for every other exception thrown in
                                                                 // ServerImpl.
                        shutdownChannels();
                        throw new StatusRuntimeException(Status.fromThrowable(e));
                    } else {// Error code for UNAVAILABLE: io exception
                            // Enters here when the replica the server was connected to crashes.
                        shutdownChannels();
                        throw new ZooKeeperServer.MissingSessionException("The replica you were connected to died.");
                    }
                }
            }
            if (ack < 5)
                ack = 0;
        }
        ack = 0;
        shutdownChannels();

        return _library.receiveAmountResponse(encryptedResponse);

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
