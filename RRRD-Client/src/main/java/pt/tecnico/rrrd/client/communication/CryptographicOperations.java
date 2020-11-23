package pt.tecnico.rrrd.client.communication;

import pt.tecnico.rrrd.client.PullResponse;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.Timestamp;
import java.util.Base64;

public class CryptographicOperations {

    private KeyStore keyStore;
    private String symmetricKeyAlias;
    private String symmetricKeyPassword;
    private String asymmetricKeyAlias;
    private String asymmetricKeyPassword;

    public CryptographicOperations(String keyStoreLocation, String keyStorePassword, String symmetricKeyAlias,
                                   String symmetricKeyPassword, String asymmetricKeyAlias, String asymmetricKeyPassword) {

        initializeKeyStore(keyStoreLocation, keyStorePassword);
        this.symmetricKeyAlias = symmetricKeyAlias;
        this.symmetricKeyPassword = symmetricKeyPassword;
        this.asymmetricKeyAlias = asymmetricKeyAlias;
        this.asymmetricKeyPassword = asymmetricKeyPassword;
    }

    private void initializeKeyStore(String keyStoreLocation, String keyStorePassword) {
        try {
            this.keyStore = KeyStore.getInstance(java.security.KeyStore.getDefaultType());
            this.keyStore.load(new FileInputStream(keyStoreLocation), keyStorePassword.toCharArray());
            System.out.println("LOADED KEYSTORE");
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    public String sign(String jsonMessage) {
        byte[] signatureBytes = null;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign((PrivateKey) this.keyStore.getKey(this.asymmetricKeyAlias, this.asymmetricKeyPassword.toCharArray()));
            signature.update(jsonMessage.getBytes());
            signatureBytes = signature.sign();
        } catch (NoSuchAlgorithmException | KeyStoreException | InvalidKeyException | UnrecoverableKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    public byte[] transform(String transformation, int mode, byte[] data, Key key) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(mode, key);
        return cipher.doFinal(data);
    }

    public String getDocument(PullResponse pullResponse) {
        byte[] documentBytes = null;
        try {
            byte[] documentKey = transform("RSA", Cipher.DECRYPT_MODE, Base64.getDecoder().decode(pullResponse.getDocumentKey()),
                    this.keyStore.getKey(this.asymmetricKeyAlias, this.asymmetricKeyPassword.toCharArray()));

            documentBytes = transform("AES", Cipher.DECRYPT_MODE, Base64.getDecoder().decode(pullResponse.getDocument()),
                    new SecretKeySpec(documentKey, "AES"));

        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | NoSuchPaddingException |
                InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return new String(documentBytes);
    }

    public String encryptDocument(String document) {
        byte[] encryptedDocument = null;
        try {
            encryptedDocument = transform("AES", Cipher.ENCRYPT_MODE, document.getBytes(),
                    this.keyStore.getKey(this.symmetricKeyAlias, this.symmetricKeyPassword.toCharArray()));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | KeyStoreException | InvalidKeyException
                | UnrecoverableKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return Base64.getEncoder().encodeToString(encryptedDocument);
    }

    public String getTimestamp() {
        return String.valueOf(new Timestamp(System.currentTimeMillis()));
    }
}
