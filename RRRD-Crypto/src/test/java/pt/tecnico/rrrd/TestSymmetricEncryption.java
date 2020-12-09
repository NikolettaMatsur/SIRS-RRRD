package pt.tecnico.rrrd;

import org.junit.Test;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import java.security.Key;

import static org.junit.Assert.assertEquals;

public class TestSymmetricEncryption {

    @Test
    public void validEncryptDecrypt() {
        String stringToEncrypt = "Hello World!";

        try {
            Key key = CryptographicOperations.getDocumentKey("password", "symmetric_key");
            byte[] encryptedBytes = CryptographicOperations.symmetricEncrypt(stringToEncrypt.getBytes(), key);
            byte[] decryptedBytes = CryptographicOperations.symmetricDecrypt(encryptedBytes, key);
            String decryptedString = new String(decryptedBytes);

            assertEquals(stringToEncrypt, decryptedString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}