package pt.tecnico.bftb.cripto;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class BFTBCriptoApp {
 
    public static byte[] hash(byte[] inputdata) {

        byte[] hash = null;
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
            sha.update(inputdata);
            hash = sha.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return hash;

    }
    public static byte[] digitalsign(byte[] inputhash, PrivateKey signprivatekey) {

        Cipher cipher;
        byte[] signature = null;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, signprivatekey);
            signature = cipher.doFinal(inputhash);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e) {
            e.printStackTrace();
        }

        return signature;

    }

    public static byte[] decrypt(byte[] encryptedString, PublicKey publicKey) {
        Cipher cipher;
        byte[] decryptedMessageHash = null;

        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            decryptedMessageHash = cipher.doFinal(encryptedString);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e) {
            e.printStackTrace();
        }

        return decryptedMessageHash;
    }
    
}
