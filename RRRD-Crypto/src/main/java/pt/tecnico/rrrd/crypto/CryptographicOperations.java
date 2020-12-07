package pt.tecnico.rrrd.crypto;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.NoSuchElementException;

public class CryptographicOperations {

    private static final String KEYSTORE_PATH = "KeyStore.jks";
    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static int SYMMETRIC_KEY_SIZE = 256;
    private static final String ASYMMETRIC_ALGORITHM = "RSA";
    private static int ASYMMETRIC_KEY_SIZE = 2048;
    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    public static long FRESHNESS_MAX_INTERVAL = 1000;

    private static KeyStore getKeyStore(String password)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        KeyStore keyStore = KeyStore.getInstance(java.security.KeyStore.getDefaultType());
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(KEYSTORE_PATH);
        keyStore.load(inputStream, password.toCharArray());

        return keyStore;
    }

    public static Key getDocumentKey(String keyStorePassword, String documentId, String documentPassword)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException { // TODO dose the client need to save the document key?

        return getKeyStore(keyStorePassword).getKey(documentId, documentPassword.toCharArray());
    }

//    private static Key createDocumentKey(String documentId) { // TODO dose the client need to save the document key?
//
//    }

    public static SecretKey createDocumentKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
        keyGenerator.init(SYMMETRIC_KEY_SIZE);

        return keyGenerator.generateKey();
    }

    public static PrivateKey getPrivateKey(String keyStorePassword, String keyAlias, String privateKeyPassword)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException {

        Key key = getKeyStore(keyStorePassword).getKey(keyAlias, privateKeyPassword.toCharArray());

        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }

        throw new NoSuchElementException("Private key " + keyAlias + " not found");
    }

    public static PublicKey getPublicKey(String keyStorePassword, String keyAlias)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {

        PublicKey key = getKeyStore(keyStorePassword).getCertificate(keyAlias).getPublicKey();

        if (key != null) {
            return key;
        }

        throw new NoSuchElementException("Public key " + keyAlias + " not found");
    }

    public static SecretKeySpec secretKey(byte[] key) {
        return new SecretKeySpec(key, SYMMETRIC_ALGORITHM);
    }

    private static byte[] transform(int mode, String transformation, byte[] data, Key key) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(mode, key);
        return cipher.doFinal(data);
    }

    public static byte[] symmetricEncrypt(byte[] data, Key key)
            throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException {

        return transform(Cipher.ENCRYPT_MODE, SYMMETRIC_ALGORITHM, data, key);
    }

    public static byte[] symmetricDecrypt(byte[] data, Key key)
            throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException {

        return transform(Cipher.DECRYPT_MODE, SYMMETRIC_ALGORITHM, data, key);
    }

    public static byte[] asymmetricEncrypt(byte[] data, Key key)
            throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException {

        return transform(Cipher.ENCRYPT_MODE, ASYMMETRIC_ALGORITHM, data, key);
    }

    public static byte[] asymmetricDecrypt(byte[] data, Key key)
            throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException {

        return transform(Cipher.DECRYPT_MODE, ASYMMETRIC_ALGORITHM, data, key);
    }

    public static String getEncryptedDocument(String keyStorePassword, String keyAlias, String documentPassword, byte[] data) throws UnrecoverableKeyException,
            CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidKeyException,
            BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException {

        Key key = getDocumentKey(keyStorePassword, keyAlias, documentPassword);
        byte[] encryptedDocument = symmetricEncrypt(data, key);

        return Base64.getEncoder().encodeToString(encryptedDocument);
    }

    public static byte[] sign(byte[] data, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException,
            SignatureException {

        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);

        return signature.sign();
    }

    public static String getSignature(String keyStorePassword, String keyAlias, String privateKeyPassword, byte[] data) throws UnrecoverableKeyException,
            CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, SignatureException,
            InvalidKeyException {

        PrivateKey privateKey = getPrivateKey(keyStorePassword, keyAlias, privateKeyPassword);
        byte[] signature = sign(data, privateKey);

        return Base64.getEncoder().encodeToString(signature);
    }

    public static boolean verifySignature(PublicKey publicKey, byte[] message, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        Signature sig = Signature.getInstance(SIGN_ALGORITHM);
        sig.initVerify(publicKey);
        sig.update(message);

        return sig.verify(signature);
    }

    public static boolean verifyTimestamp(String timestamp) throws ParseException {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        Date parsedDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS").parse(timestamp);
        Timestamp messageTimeStamp = new Timestamp(parsedDate.getTime());

        return now.getTime() - messageTimeStamp.getTime() < FRESHNESS_MAX_INTERVAL;
    }

    public static String getTimestamp() {
        return String.valueOf(new Timestamp(System.currentTimeMillis()));
    }
}
