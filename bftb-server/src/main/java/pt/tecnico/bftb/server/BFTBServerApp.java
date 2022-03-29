package pt.tecnico.bftb.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
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

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(
                    "/Users/rodrigopinto/Desktop/IST/Masters/3º Quarter/SEC/Project/BFTB/BFTB-SEC/certificates/server/ServerKeyStore.jks"),
                    "serverkeystore".toCharArray());

            System.out.println(ks.getKey("server", "serverkeystore".toCharArray()));
        } catch (Exception e) {
            System.out.println(e);
        }

        // Implementation of server.
        final BindableService impl = new BFTBImpl();
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