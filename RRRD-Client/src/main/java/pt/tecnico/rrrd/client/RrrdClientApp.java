package pt.tecnico.rrrd.client;

import pt.tecnico.rrrd.client.communication.CryptographicOperations;
import pt.tecnico.rrrd.client.communication.MessageFactory;
import pt.tecnico.rrrd.client.communication.MessageHandler;

import javax.crypto.Cipher;

public class RrrdClientApp {

    public RrrdClientApp(String keyStoreLocation, String keyStorePassword) {

        CryptographicOperations cryptographicOperations = new CryptographicOperations(keyStoreLocation, keyStorePassword,
                "symmetric_key", "password", "asymmetric_keys",
                "password");

        MessageFactory messageFactory = new MessageFactory(cryptographicOperations);
        MessageHandler messageHandler = new MessageHandler(cryptographicOperations);

//        System.out.println(messageFactory.createPullRequestMessage("1"));
        System.out.println(messageFactory.createPushRequestMessage("1", "abcde"));


    }

    public static void main(String[] args) {
        new RrrdClientApp(args[0], args[1]);
    }
}