package pt.tecnico.rrrd.server;

import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.crypto.CryptographicOperations;
import pt.tecnico.rrrd.server.utils.Utils;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
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
        try {
            logger.info(String.format("Received Pull Request: {Document Id: %s, Timestamp: %s}\n", request.getMessage().getDocumentId(), request.getMessage().getTimestamp()));

            // Verify signature and ts
            PublicKey publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys"); // TODO should be the users public key
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());

            // TODO verify user permissions

            if (verifySig && verifyTimestamp) {
                String key = ""; // TODO get file key encrypted with users public key

                String encryptedDocumentData = Files.readString(Paths.get("/home/" + Utils.getUserName() + "/sync/server/" + request.getMessage().getDocumentId()), StandardCharsets.UTF_8);

                PullResponse pullResponse = PullResponse.newBuilder().
                        setDocument(encryptedDocumentData.trim()).
                        setDocumentKey(key).
                        build();

                responseObserver.onNext(pullResponse);
                responseObserver.onCompleted();
            } else {
                String message = !verifySig ? "Invalid Signature." : "Invalid TimeStamp.";
                logger.info(message + " Aborting operation.");

                throw new InvalidParameterException(message);
            }

        } catch (Exception e) {
            responseObserver.onError(Status.DATA_LOSS
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
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
            } else {
                logger.info(String.format("Signature and Timestamp verified. Writing encrypted file: sync/%s", pushMessage.getDocumentId()));

                PrintWriter writer = new PrintWriter("/home/" + Utils.getUserName() + "/sync/server/" + pushMessage.getDocumentId(), StandardCharsets.UTF_8);
                writer.println(pushMessage.getEncryptedDocument());
                writer.close();

                PushResponse pushResponse = PushResponse.newBuilder().setMessage("OK").build();

                responseObserver.onNext(pushResponse);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            PushResponse pushResponse = PushResponse.newBuilder().setMessage(e.getMessage()).build();
            responseObserver.onNext(pushResponse);
            responseObserver.onCompleted();
        }
    }


}
