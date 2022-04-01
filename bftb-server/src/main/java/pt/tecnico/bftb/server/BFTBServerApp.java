package pt.tecnico.bftb.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.BindableService;
import java.util.Scanner;

public class BFTBServerApp {

    static final int SERVER_NUMBER_OF_ARGUMENTS = 1;

    static PrivateKey serverPrivateKey;
    static PublicKey serverPublicKey;

    public static void main(String[] args) throws IOException, InterruptedException {

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try{
                    Thread.sleep(200);
                    System.out.println("\nA fatal error occurred in the server.");
                    System.out.println("Closing...");
                }
                catch(InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        });


        System.out.println("Byzantine Fault Tolerant Banking server");

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }
        if (args.length < SERVER_NUMBER_OF_ARGUMENTS) {
            System.err.println("Argument(s) missing!");
            return;
        }

        final int port = Integer.parseInt(args[0]);

        try {

            String originPath = System.getProperty("user.dir");
            Path path = Paths.get(originPath);

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(path.getParent() + "/certificates/keys/GlobalKeyStore.jks"),
                    "keystore".toCharArray());

            Certificate cert = ks.getCertificate("server");

            serverPublicKey = cert.getPublicKey();
            System.out.println(serverPublicKey);

            PrivateKeyEntry priv = (KeyStore.PrivateKeyEntry) ks.getEntry("server",
                    new KeyStore.PasswordProtection(("keystore").toCharArray()));

            serverPrivateKey = priv.getPrivateKey();

        } catch (Exception e) {
            System.out.println(e);
        }

        // Implementation of server.
        final BindableService impl = new BFTBImpl(serverPrivateKey);
        Server server = ServerBuilder.forPort(port).addService(impl).build();

        server.start();
        System.out.println("Server started");

        new Thread(() -> {
            System.out.println("<Press enter to shutdown>");
            new Scanner(System.in).nextLine();

            server.shutdown();
        }).start();

        server.awaitTermination();
    }

}