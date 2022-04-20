package pt.tecnico.bftb.client;

import java.io.FileInputStream;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.netty.util.internal.ThreadLocalRandom;
import org.apache.zookeeper.server.ZooKeeperServer;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.library.DetectedReplayAttackException;
import pt.tecnico.bftb.library.ManipulatedPackageException;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

public class BFTBClientApp {

    static PublicKey publicKey;
    static PrivateKey privateKey;
    static ByteString encodedPublicKey;

    public static void main(String[] args) {

        final String zooHost = args[0];
        final String zooPort = args[1];
        String serverHost = "";
        int serverPort = 0;
        ZKNaming zkNaming = null;
        String serverInfo = "";
        ArrayList<ZKRecord> zkRecordsList;
        String[] splittedServerInfo;
        int randomServerIndex;
        BFTBFrontend frontend = null;
        Path path = null;
        KeyStore ks = null;
        char serverLastChar = ' ';
        int ack = 0;
        int wts = 0;

        System.out.println(BFTBClientApp.class.getSimpleName());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println("\nA fatal error occurred in the client.");
                    System.out.println("Closing...");
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // receive and print arguments from POM
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        String publicKeyString = null;
        String name = System.console().readLine(Label.CLIENT_NAME);

        while (!name.replaceAll("[0-9]", "").equals("user")) {
            System.out.println("Please insert a valid username.");
            name = System.console().readLine(Label.CLIENT_NAME);
        }

        char lastChar = name.charAt(name.length() - 1);

        int counter = 3; // User has 3 attempts to provide correct password.
        while (counter != 0) {
            String inputPassword = System.console().readLine(Label.PASSWORD);

            if (inputPassword.equals("keystoreuser" + lastChar)) {
                System.out.printf("Welcome to the Byzantine Fault Tolerant Banking Client App %s.%n", name);
                break;
            } else {
                if (counter == 1) {
                    System.out.println("User authentication failed.");
                    System.out.println("Closing...");
                    return;
                }
                System.out.println("Wrong password.");
                counter -= 1;
                System.out.printf("You have %d attempt(s) left.%n", counter);
            }
        }

        zkNaming = new ZKNaming(zooHost, zooPort);

        /*---------------------------------- Public an Private keys generation ----------------------------------*/

        try {

            String originPath = System.getProperty("user.dir");
            path = Paths.get(originPath);

            ks = KeyStore.getInstance("JKS");
            ks.load((new FileInputStream(
                    path.getParent() + "/certificates/user" + lastChar + "keystore.jks")),
                    ("keystoreuser" + lastChar).toCharArray());

            publicKey = ks.getCertificate(name).getPublicKey();

            privateKey = ((KeyStore.PrivateKeyEntry) ks.getEntry(name,
                    new KeyStore.PasswordProtection(("keystoreuser" + lastChar).toCharArray()))).getPrivateKey();

            encodedPublicKey = ByteString.copyFrom(publicKey.getEncoded());

        } catch (Exception e) {
            System.out.println("There isn't a public key pair registered in the system for this user.");
            System.out.println("User authentication failed.");
            System.out.println("Closing...");
            return;
        }

        /*---------------------------------- Public an Private keys generation ----------------------------------*/

        frontend = new BFTBFrontend(zooHost, zooPort, privateKey, publicKey);// Frontend server
        // implementation.
        System.out.println(
                "I'm user" + lastChar + " and I am connected to the server running in port number " + serverPort + ".");

        /*---------------------------------- List of Commands to the server ----------------------------------*/
        System.out.println(Label.TYPE_HELP);
        boolean doesUserHaveAccountRegistered = false;

        while (true) {
            String command = System.console().readLine(Label.COMMANDPROMPT);
            String[] splittedCommand = command.trim().split(" ");

            try {
                switch (splittedCommand[0]) {
                    case "open_account":

                        try {

                            OpenAccountResponse response = frontend.openAccount(encodedPublicKey, name);
                            publicKeyString = response.getPublicKey();
                            System.out.println(response.getResponse());
                            doesUserHaveAccountRegistered = true;

                        } catch (ManipulatedPackageException mpe) {
                            System.out.println(mpe.getMessage());
                        }
                        break;

                    case "send_amount":

                        if (!doesUserHaveAccountRegistered) {
                            System.out.println("You have to register an account first.");
                            continue;
                        }

                        if (splittedCommand.length != 3) {
                            System.out.println(Label.INVALID_ARGS_SND_AMT);
                            continue;
                        }

                        try {
                            System.out.println(frontend
                                    .sendAmount(publicKeyString, splittedCommand[1],
                                            Integer.parseInt(splittedCommand[2]))
                                    .getResponse());
                        } catch (ManipulatedPackageException mpe) {
                            System.out.println(mpe.getMessage());
                        }
                        break;

                    case "check_account":

                        if (!doesUserHaveAccountRegistered) {
                            System.out.println("You have to register an account first.");
                            continue;
                        }

                        if (splittedCommand.length != 2) {
                            System.out.println(Label.INVALID_ARGS_CHECK_ACCOUNT);
                            continue;
                        }
                        CheckAccountResponse check_account_response;
                        try {
                            check_account_response = frontend.checkAccount(splittedCommand[1], publicKeyString);
                            System.out.println(Label.BALANCE + check_account_response.getBalance());
                            for (String pendingTransaction : check_account_response.getPendingList()) {
                                System.out.println(pendingTransaction);
                            }
                        } catch (ManipulatedPackageException mpe) {
                            System.out.println(mpe.getMessage());

                        }

                        break;

                    case "receive_amount":

                        if (!doesUserHaveAccountRegistered) {
                            System.out.println("You have to register an account first.");
                            continue;
                        }

                        if (splittedCommand.length != 4) {
                            System.out.println(Label.INVALID_ARGS_RCV_AMOUNT);
                            continue;
                        }
                        String answer = splittedCommand[3];
                        if (answer.equals("yes") || answer.equals("Yes") || answer.equals("no")
                                || answer.equals("No")) {
                            boolean accept = splittedCommand[3].equals("yes") || splittedCommand[3].equals("Yes");
                            try {
                                System.out.println(frontend.receiveAmount(publicKeyString, splittedCommand[1],
                                        Integer.parseInt(splittedCommand[2]), accept).getResult());
                            } catch (ManipulatedPackageException mpe) {
                                System.out.println(mpe.getMessage());
                            }
                        } else {
                            System.out.println(Label.INVALID_ARGS_RCV_AMOUNT_ANSWER);
                        }
                        break;

                    case "audit":

                        if (!doesUserHaveAccountRegistered) {
                            System.out.println("You have to register an account first.");
                            continue;
                        }

                        if (splittedCommand.length != 2) {
                            System.out.println(Label.INVALID_ARGS_AUDIT);
                            continue;
                        }

                        List<String> list = null;
                        try {
                            list = frontend.audit(splittedCommand[1], publicKeyString).getSetList();
                        } catch (ManipulatedPackageException mpe) {
                            System.out.println(mpe.getMessage());
                        }

                        for (String transaction : list) {
                            System.out.println(transaction);
                        }
                        break;

                    case "search_keys":

                        if (!doesUserHaveAccountRegistered) {
                            System.out.println("You have to register an account first.");
                            continue;
                        }

                        System.out.println("List of public keys available:");
                        List<String> list_search_keys = frontend.searchKeys(publicKeyString).getResultList();

                        for (String publicKeyList : list_search_keys) {
                            int id = Integer.parseInt(publicKeyList.substring(publicKeyList.length() - 1));

                            System.out.println("User" + String.valueOf(id) + " account:");
                            System.out.println("\t" + publicKeyList + "");
                        }
                        break;

                    case "help":
                        System.out.println(Label.HELPGUIDE);
                        break;

                    case "exit":
                        System.exit(0);
                        break;

                    default:
                        System.out.println(Label.INVALIDCOMMAND);

                }
            } catch (DetectedReplayAttackException drae) {
                System.out.println(drae.getMessage());
            } catch (ZooKeeperServer.MissingSessionException e) {
                e.printStackTrace();
            } catch (StatusRuntimeException sre) {// This is where the exceptions from grpc are caught.
                System.out.println(sre.getMessage());
            } catch (NumberFormatException nfe) {
                System.out.println(Label.INVALID_AMOUNT_TYPE);
            } catch (ManipulatedPackageException e) {
                e.printStackTrace();
            } catch (PacketDropAttack pda) {
                System.out.println(pda.getMessage());
            }

        }

    }
}