package pt.tecnico.rrrd.crypto;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
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
    public static long FRESHNESS_MAX_INTERVAL = 5000;

    public static KeyStore getKeyStore(String password)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        KeyStore keyStore = KeyStore.getInstance(java.security.KeyStore.getDefaultType());
        keyStore.load(new FileInputStream(KEYSTORE_PATH), password.toCharArray());

        return keyStore;
    }

    public static SecretKey convertToSymmetricKey(byte[] key) {
        return new SecretKeySpec(key, SYMMETRIC_ALGORITHM);
    }

    public static PublicKey convertToPublicKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance(ASYMMETRIC_ALGORITHM).generatePublic(new X509EncodedKeySpec(key));
    }

    public static Key getDocumentKey(String keyStorePassword, String documentId)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {

        return getKeyStore(keyStorePassword).getKey(documentId, keyStorePassword.toCharArray());
    }

    public static void storeDocumentKey(String keyStorePassword, String documentId, SecretKey key) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException {

        KeyStore.SecretKeyEntry secret = new KeyStore.SecretKeyEntry(key);
        KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(keyStorePassword.toCharArray());
        KeyStore keyStore = getKeyStore(keyStorePassword);

        keyStore.setEntry(documentId, secret, password);

        FileOutputStream out = new FileOutputStream(KEYSTORE_PATH);
        keyStore.store(out, keyStorePassword.toCharArray());
        out.close();
    }

    public static SecretKey createDocumentKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
        keyGenerator.init(SYMMETRIC_KEY_SIZE);

        return keyGenerator.generateKey();
    }

    public static PrivateKey getPrivateKey(String keyStorePassword, String keyAlias)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException {

        Key key = getKeyStore(keyStorePassword).getKey(keyAlias, keyStorePassword.toCharArray());

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


    public static String getStringPubKey(PublicKey pubKey){
        return Base64.getEncoder().encodeToString(pubKey.getEncoded());
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

    public static String getEncryptedDocument(String keyStorePassword, String documentId, byte[] data) throws UnrecoverableKeyException,
            CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidKeyException,
            BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException {

        Key key = getDocumentKey(keyStorePassword, documentId);
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

    public static String getSignature(String keyStorePassword, String keyAlias, byte[] data) throws UnrecoverableKeyException,
            CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, SignatureException,
            InvalidKeyException {

        PrivateKey privateKey = getPrivateKey(keyStorePassword, keyAlias);
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
