package pt.tecnico.rrrd;

import org.junit.Test;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.Assert.assertEquals;

public class TestAymmetricEncryption {

    @Test
    public void validPrivateEncryptPublicDecrypt() {
        String stringToEncrypt = "Hello World!";

        try {
            PrivateKey privateKey = CryptographicOperations.getPrivateKey("password", "asymmetric_keys", "password");
            byte[] encryptedBytes = CryptographicOperations.asymmetricEncrypt(stringToEncrypt.getBytes(), privateKey);
            PublicKey publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys");
            byte[] decryptedBytes = CryptographicOperations.asymmetricDecrypt(encryptedBytes, publicKey);
            String decryptedString = new String(decryptedBytes);

            assertEquals(stringToEncrypt, decryptedString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void validPublicEncryptPrivateDecrypt() {
        String stringToEncrypt = "Hello World!";

        try {
            PublicKey publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys");
            byte[] encryptedBytes = CryptographicOperations.asymmetricEncrypt(stringToEncrypt.getBytes(), publicKey);
            PrivateKey privateKey = CryptographicOperations.getPrivateKey("password", "asymmetric_keys", "password");
            byte[] decryptedBytes = CryptographicOperations.asymmetricDecrypt(encryptedBytes, privateKey);
            String decryptedString = new String(decryptedBytes);

            assertEquals(stringToEncrypt, decryptedString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
