package pt.tecnico.rrrd;

import org.junit.Test;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestTimestampInterval {

    @Test
    public void validTimestampInterval() {

        try {
            boolean valid = CryptographicOperations.verifyTimestamp(String.valueOf(new Timestamp(System.currentTimeMillis())));

            assertTrue(valid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void invalidTimestampInterval() {
        try {
            boolean valid = CryptographicOperations.verifyTimestamp(String.valueOf(new Timestamp(System.currentTimeMillis() - (CryptographicOperations.FRESHNESS_MAX_INTERVAL + 10))));

            assertFalse(valid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
