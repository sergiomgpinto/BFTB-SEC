package pt.tecnico.bftb.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.BindableService;

import java.util.Scanner;

public class BFTBServerApp {

    static final int SERVER_NUMBER_OF_ARGUMENTS = 1;

    static PrivateKey serverPrivateKey;
    static PublicKey serverPublicKey;
    static String finalPath = "";
    static String port = "";
    static ZKNaming zkNaming = null;

    public static void main(String[] args) throws IOException, InterruptedException {

        String mainPath = "/nodes";// path to main node in zookeeper
        final String zooHost = args[0];// zookeeper server host
        final String zooPort = args[1];// zookeeper server port
        final String host = args[2];// server host

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    if (zkNaming != null) {
                        try {
                            zkNaming.unbind(finalPath, host, String.valueOf(port));
                        }
                        catch (ZKNamingException e) {
                            //Should never happen.
                        }
                    }
                    System.out.println("\nA fatal error occurred in the server.");
                    System.out.println("Closing...");
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Scanner scanner = new Scanner(System.in);
        String serverName = scanner.nextLine();

        char lastChar = serverName.charAt(serverName.length() - 1);
        port = "808" + lastChar;
        finalPath = mainPath + "/Server" + lastChar;

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }
        if (args.length < SERVER_NUMBER_OF_ARGUMENTS) {
            System.err.println("Argument(s) missing!");
            return;
        }

        try {

            zkNaming = new ZKNaming(zooHost, zooPort);// zookeeper server

            String originPath = System.getProperty("user.dir");
            Path path = Paths.get(originPath);

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(path.getParent() + "/certificates/server" + lastChar + "keystore.jks"),
                    ("keystoreserver" + lastChar).toCharArray());

            Certificate cert = ks.getCertificate("server" + lastChar);
            serverPublicKey = cert.getPublicKey();

            PrivateKeyEntry priv = (KeyStore.PrivateKeyEntry) ks.getEntry("server" + lastChar,
                    new KeyStore.PasswordProtection(("keystoreserver" + lastChar).toCharArray()));

            serverPrivateKey = priv.getPrivateKey();

            zkNaming.rebind(finalPath, host, port);

        System.out.println("Byzantine Fault Tolerant Banking server");

        System.out.println("Hello, I'm a server running on port number " + port);
        // Implementation of server.
        final BindableService impl = new BFTBImpl(serverPrivateKey, serverPublicKey);
        Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();

        server.start();

        server.awaitTermination();
        } catch (FileNotFoundException | NoSuchAlgorithmException | UnrecoverableEntryException | CertificateException
                | KeyStoreException e) {
            System.out.println(e);
            return;
        } catch (ZKNamingException zkne) {
            System.out.println("An error occurred in a zookeeper service while trying to connect the server");
            return;
        }
    }

}