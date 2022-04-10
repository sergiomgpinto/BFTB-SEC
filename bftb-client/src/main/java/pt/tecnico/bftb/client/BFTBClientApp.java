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
import java.util.Collection;
import java.util.List;

import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.netty.util.internal.ThreadLocalRandom;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
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
        char lastChar = name.charAt(name.length() - 1);
        String mainPath = "/nodes";

        try {
            zkNaming = new ZKNaming(zooHost, zooPort);
            zkRecordsList = new ArrayList<>(zkNaming.listRecords(mainPath));
            randomServerIndex = ThreadLocalRandom.current().nextInt(0, zkRecordsList.size());
            serverInfo = zkRecordsList.get(randomServerIndex).getURI();
            splittedServerInfo = serverInfo.split(":");
            serverHost = splittedServerInfo[0];
            serverPort = Integer.parseInt(splittedServerInfo[1]);
            serverLastChar = Integer.toString(serverPort).charAt(Integer.toString(serverPort).length() - 1);

        } catch (ZKNamingException zkne) {
            System.out.println("An error occurred in a zookeeper service while trying to connect the server");
            return;
        }

        /*---------------------------------- Public an Private keys generation ----------------------------------*/

        try {

            String originPath = System.getProperty("user.dir");
            path = Paths.get(originPath);

            ks = KeyStore.getInstance("JKS");
            ks.load((new FileInputStream(
                    path.getParent() + "/certificates/user" + lastChar + "keystore.jks")),
                    ("keystoreuser" + lastChar).toCharArray());
            Certificate cert = ks.getCertificate(name);

            publicKey = cert.getPublicKey();

            PrivateKeyEntry priv = (KeyStore.PrivateKeyEntry) ks.getEntry(name,
                    new KeyStore.PasswordProtection(("keystoreuser" + lastChar).toCharArray()));

            privateKey = priv.getPrivateKey();

            encodedPublicKey = ByteString.copyFrom(publicKey.getEncoded());

        } catch (Exception e) {
            System.out.println(e);
        }

        /*---------------------------------- Public an Private keys generation ----------------------------------*/

        frontend = new BFTBFrontend(serverHost, serverPort, privateKey, publicKey);// Frontend server
                                                                                   // implementation.
        System.out.println(
                "I'm user" + lastChar + " and I am connected to the server running in port number " + serverPort);

        /*---------------------------------- Registration into the server ----------------------------------*/
        OpenAccountResponse response = null;

        try {
            response = frontend.openAccount(encodedPublicKey);
            publicKeyString = response.getPublicKey();
            System.out.println(response.getResponse());
        } catch (ManipulatedPackageException mpe) {
            System.out.println(mpe.getMessage());
        }

        /*---------------------------------- Registration into the server ----------------------------------*/
        System.out.println(Label.TYPE_HELP);
        while (true) {
            String command = System.console().readLine(Label.COMMANDPROMPT);
            String[] splittedCommand = command.trim().split(" ");

            try {
                switch (splittedCommand[0]) {
                    case "open_account":
                        // Argument public key is predefined since each user only has one account.

                        response = null;

                        try {
                            System.out.println(frontend.openAccount(encodedPublicKey).getResponse());
                        } catch (ManipulatedPackageException mpe) {
                            System.out.println(mpe.getMessage());
                        }

                        break;

                    case "send_amount":
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
                        if (splittedCommand.length != 2) {
                            System.out.println(Label.INVALID_ARGS_CHECK_ACCOUNT);
                            continue;
                        }
                        CheckAccountResponse check_account_response;
                        try {
                            check_account_response = frontend.checkAccount(splittedCommand[1]);
                            System.out.println(Label.BALANCE + check_account_response.getBalance());
                            for (String pendingTransaction : check_account_response.getPendingList()) {
                                System.out.println(pendingTransaction);
                            }
                        } catch (ManipulatedPackageException mpe) {
                            System.out.println(mpe.getMessage());

                        }

                        break;

                    case "receive_amount":
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
                        if (splittedCommand.length != 2) {
                            System.out.println(Label.INVALID_ARGS_AUDIT);
                            continue;
                        }

                        List<String> list = null;
                        try {
                            list = frontend.audit(splittedCommand[1]).getSetList();
                        } catch (ManipulatedPackageException mpe) {
                            System.out.println(mpe.getMessage());
                        }

                        for (String transaction : list) {
                            System.out.println(transaction);
                        }
                        break;

                    case "search_keys":
                        System.out.println("List of public keys available:");
                        List<String> list_search_keys = frontend.searchKeys().getResultList();

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

            } catch (StatusRuntimeException e) {// This is where the exceptions from grpc are caught.
                try {
                    zkNaming.unbind(mainPath + "/Server" + serverLastChar, serverInfo);

                    zkRecordsList = new ArrayList<>(zkNaming.listRecords(mainPath));

                    randomServerIndex = ThreadLocalRandom.current().nextInt(0, zkRecordsList.size());

                    serverInfo = zkRecordsList.get(randomServerIndex).getURI();
                    splittedServerInfo = serverInfo.split(":");
                    serverHost = splittedServerInfo[0];
                    serverPort = Integer.parseInt(splittedServerInfo[1]);
                    serverLastChar = serverInfo.charAt(serverInfo.length() - 1);
                    frontend = new BFTBFrontend(serverHost, serverPort, privateKey, publicKey);

                    System.out.println("\nServer died.\nConnecting to another replica...");
                    System.out.println(
                            "Connected to the server running in port number " + serverPort + "!\n");

                } catch (ZKNamingException e1) {
                    System.out.println("Error while trying to connect to other replica.");
                } catch (Exception e2) {
                    e2.getStackTrace(); // add a case error
                }

            } catch (NumberFormatException nfe) {
                System.out.println(Label.INVALID_AMOUNT_TYPE);
            }

        }

    }
}