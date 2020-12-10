package pt.tecnico.rrrd.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;
import java.util.logging.Logger;

public class RrrdServerService extends RemoteServerGrpc.RemoteServerImplBase {
    private final Logger logger;

    public RrrdServerService(){
        this.logger = Logger.getLogger(RrrdServerApp.class.getName());
    }

    @Override
    public void pull(PullRequest request, StreamObserver<PullResponse> responseObserver) {
        System.out.println("Received pull request.");

        PullResponse pullResponse = PullResponse.newBuilder().setDocument("Doc 1").setDocumentKey("Key1").build();

        responseObserver.onNext(pullResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void push(PushRequest request, StreamObserver<PushResponse> responseObserver) {
        try {
            PushMessage pushMessage = request.getMessage();
            byte[] signature = Base64.getDecoder().decode(request.getSignature());
            PublicKey publicKey;


            logger.info(String.format("Received Push Request: {Document Id: %s, Timestamp: %s}\n", pushMessage.getDocumentId(), pushMessage.getTimestamp()));

            publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys");
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, pushMessage.toByteArray(), signature);
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(pushMessage.getTimestamp());
            if (!verifySig || !verifyTimestamp) {
                String message = !verifySig ? "Invalid Signature." : "Invalid TimeStamp.";
                logger.info(message + " Aborting operation.");
                PushResponse pushResponse = PushResponse.newBuilder().setMessage(message).build();
                responseObserver.onNext(pushResponse);
                responseObserver.onCompleted();
            }
            logger.info(String.format("Signature and Timestamp verified. Writing encrypted file: sync/%s",pushMessage.getDocumentId() ));

            PrintWriter writer = new PrintWriter("sync/" + pushMessage.getDocumentId(), StandardCharsets.UTF_8);
            writer.println(pushMessage.getEncryptedDocument());
            writer.close();

            PushResponse pushResponse = PushResponse.newBuilder().setMessage("OK").build();

            responseObserver.onNext(pushResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            PushResponse pushResponse = PushResponse.newBuilder().setMessage(e.getMessage()).build();
            responseObserver.onNext(pushResponse);
            responseObserver.onCompleted();
        }
    }


}
