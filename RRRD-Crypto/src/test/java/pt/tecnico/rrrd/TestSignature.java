package pt.tecnico.rrrd;

import org.junit.Test;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.Assert.assertTrue;

public class TestSignature {

    @Test
    public void validSignature() {
        String stringToEncrypt = "Hello World!";

        try {
            PrivateKey privateKey = CryptographicOperations.getPrivateKey("password", "asymmetric_keys");
            byte[] signature = CryptographicOperations.sign(stringToEncrypt.getBytes(), privateKey);
            PublicKey publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys");
            boolean valid = CryptographicOperations.verifySignature(publicKey, stringToEncrypt.getBytes(), signature);

            assertTrue(valid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
