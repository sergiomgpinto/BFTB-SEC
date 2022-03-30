package pt.tecnico.bftb.client;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;

public class BFTBClientApp {

    static PublicKey publicKey;
    static PrivateKey privateKey;
    static ByteString encodedPublicKey;

    public static void main(String[] args) {
        System.out.println(BFTBClientApp.class.getSimpleName());

        // receive and print arguments from POM
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        String publicKeyString = null;
        /*---------------------------------- Public an Private keys generation ----------------------------------*/
        String name = System.console().readLine(Label.CLIENT_NAME);
        char[] charName = name.toCharArray();
        int lastNameIndex = name.length() - 1;
        try {

            String originPath = System.getProperty("user.dir");
            Path path = Paths.get(originPath);

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(
                    path.getParent() + "/certificates/javacert/users/user" + charName[lastNameIndex] + "keystore.jks"),
                    ("keystore" + charName[lastNameIndex]).toCharArray());

            privateKey = (PrivateKey) ks.getKey(name, ("keystore" + charName[lastNameIndex]).toCharArray());

            byte[] keyBytes = Files.readAllBytes(
                    Paths.get(path.getParent() + "/certificates/javacert/users/user" + charName[lastNameIndex]
                            + "pub.key"));

            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            publicKey = kf.generatePublic(spec);

        } catch (Exception e) {
            System.out.println(e);
        }

        /*---------------------------------- Public an Private keys generation ----------------------------------*/

        BFTBFrontend frontend = new BFTBFrontend(host, port, privateKey);// Frontend server implementation.

        /*---------------------------------- Registration into the server ----------------------------------*/
        OpenAccountResponse response = frontend.openAccount(encodedPublicKey);
        publicKeyString = response.getPublicKey();
        System.out.println(response.getResponse());
        /*---------------------------------- Registration into the server ----------------------------------*/
        System.out.println(Label.TYPE_HELP);
        while (true) {
            String command = System.console().readLine(Label.COMMANDPROMPT);
            String[] splittedCommand = command.trim().split(" ");

            try {
                switch (splittedCommand[0]) {
                    case "open_account":
                        // Argument public key is predefined since each user only has one account.
                        System.out.println(frontend.openAccount(encodedPublicKey).getResponse());
                        break;

                    case "send_amount":
                        if (splittedCommand.length != 3) {
                            System.out.println(Label.INVALID_ARGS_SND_AMT);
                            continue;
                        }
                        System.out.println(frontend
                                .sendAmount(publicKeyString, splittedCommand[1], Integer.parseInt(splittedCommand[2]))
                                .getResponse());
                        break;

                    case "check_account":
                        if (splittedCommand.length != 2) {
                            System.out.println(Label.INVALID_ARGS_CHECK_ACCOUNT);
                            continue;
                        }
                        CheckAccountResponse check_account_response = frontend.checkAccount(splittedCommand[1]);
                        System.out.println(Label.BALANCE + check_account_response.getBalance());
                        for (String pendingTransaction : check_account_response.getPendingList()) {
                            System.out.println(pendingTransaction);
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
                            System.out.println(frontend.receiveAmount(publicKeyString, splittedCommand[1],
                                    Integer.parseInt(splittedCommand[2]), accept).getResult());
                        } else {
                            System.out.println(Label.INVALID_ARGS_RCV_AMOUNT_ANSWER);
                        }
                        break;

                    case "audit":
                        if (splittedCommand.length != 2) {
                            System.out.println(Label.INVALID_ARGS_AUDIT);
                            continue;
                        }

                        List<String> list = frontend.audit(splittedCommand[1]).getSetList();

                        for (String transaction : list) {
                            System.out.println(transaction);
                        }
                        break;

                    case "search_keys":
                        System.out.println("List of public keys available:");
                        List<String> list_search_keys = frontend.searchKeys().getResultList();

                        for (String publicKey : list_search_keys) {
                            int id = Integer.parseInt(publicKey.substring(publicKey.length() - 1));

                            System.out.println("User" + String.valueOf(id) + " account:");
                            System.out.println("\t" + publicKey + "");
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
                System.out.println(e.getMessage());
            } catch (NumberFormatException nfe) {
                System.out.println(Label.INVALID_AMOUNT_TYPE);
            }

        }

    }
}