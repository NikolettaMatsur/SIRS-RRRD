package pt.tecnico.rrrd.client;

import pt.tecnico.rrrd.crypto.CryptographicOperations;
import pt.tecnico.rrrd.client.communication.MessageFactory;


import java.security.*;
import java.util.Base64;

public class RrrdClientApp {

    public RrrdClientApp() {
        System.out.println(MessageFactory.createPullRequestMessage("12"));
    }

    public static void main(String[] args) {
        new RrrdClientApp();
    }
}