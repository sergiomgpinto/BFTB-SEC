package pt.tecnico.bftb.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class ProofOfWorkService {

    // As explained in the properties file, this will be the number of zeros required at the
    // beginning of the hash solution.

    final int _numberOfZeros;

    public ProofOfWorkService() {
        Properties prop = new Properties();
        String originPath = System.getProperty("user.dir");
        try {
            InputStream in = new FileInputStream(originPath + "/../resources/config.properties");
            prop.load(in);
        }
        catch (FileNotFoundException fnfe) {
            System.out.println("Properties file in resources directory not found.");
        }
        catch (IOException ioe) {
            // Should never happen.
        }
        _numberOfZeros = Integer.parseInt(prop.getProperty("numberOfZeros"));
    }

    public boolean verifySolution(String challenge, String solution) {
        byte[] digestBytes;
        MessageDigest digest;
        BigInteger big;

        try {
            String input = challenge + solution;
            digest = MessageDigest.getInstance("SHA-256");
            digestBytes = digest.digest(input.getBytes());

            big = new BigInteger(digestBytes);
            String digestBytesBinaryString = big.toString(2);

            if (_numberOfZeros < digestBytesBinaryString.length()) {
                // Starts at 1 because a minus sign may be appended at the beginning.
                if (!digestBytesBinaryString.substring(1,_numberOfZeros + 1).contains("1")) {
                    return true;
                }
            }
            return false;
        }
        catch (NoSuchAlgorithmException nsae) {
            System.out.println(nsae.getMessage());
        }
        return true;
    }
}
