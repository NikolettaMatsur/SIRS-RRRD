package pt.tecnico.rrrd.server;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerBlockingStub;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerStub;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.crypto.CryptographicOperations;
import pt.tecnico.rrrd.crypto.DataOperations;

import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Scanner;
import java.util.logging.Logger;


public class RrrdBackupClientAPI {

    private final BackupServerBlockingStub blockingStub;
    private final BackupServerStub asyncStub;
    private static String keyStorePassword;
    private final Logger logger;

    public RrrdBackupClientAPI(BackupServerBlockingStub blockingStub, BackupServerStub asyncStub, String keyStorePassword) {
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
        this.keyStorePassword = keyStorePassword;
        this.logger = Logger.getLogger(RrrdBackupClientAPI.class.getName());

    }

    public void getVersions() {
        GetVersionsResponse getVersionsResponse = blockingStub.getVersions(GetVersionsRequest.newBuilder().build());

        System.out.println("Versions: ");

        for (Version version : getVersionsResponse.getVersionListList()) {
            System.out.printf("\tNumber: %d, Date: %s, Number of Files: %d\n", version.getNumber(), version.getDate(), version.getNumberOfFiles());
        }
    }

    public void update() throws IOException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, SignatureException, InvalidKeyException {

        UpdateMessage.Builder updateMessageBuilder = UpdateMessage.newBuilder();
        File dir = new File("sync");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            this.logger.info(String.format("Sending new update request for %d files.", directoryListing.length));
            for (File child : directoryListing) {
                Scanner sc = new Scanner(child);
                String data = !sc.hasNext() ? "" : sc.nextLine();
                Document document = Document.newBuilder().setDocumentId(child.getName()).setEncryptedDocument(data).build();
                updateMessageBuilder.addDocumentList(document);
            }
        }
        updateMessageBuilder.setTimestamp(CryptographicOperations.getTimestamp());
        UpdateMessage updateMessage = updateMessageBuilder.build();
        UpdateRequest updateRequest = UpdateRequest.newBuilder()
                .setUpdateMessage(updateMessageBuilder.build())
                .setSignature(CryptographicOperations.getSignature(this.keyStorePassword, "asymmetric_keys", updateMessage.toByteArray()))
                .build();

        UpdateResponse updateResponse = null;
        try {
            updateResponse = blockingStub.update(updateRequest);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DATA_LOSS) {
                this.logger.severe("Invalid Signature or Timestamp. Aborting operation.");
                return;
            }
        }
        assert updateResponse != null;
        String status = updateResponse.getStatus();
        this.logger.info(String.format("Received update response - Status: %s\n", status));

    }

    public void restore(int version) {
        logger.info("Received restore response.");
        RestoreRequest restoreRequest = RestoreRequest.newBuilder().setVersion(version).build();
        RestoreResponse restoreResponse = null;
        try {
            restoreResponse = blockingStub.restore(restoreRequest);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                this.logger.severe("Version not found. Aborting.");
                return;
            }
        }
        assert restoreResponse != null;
        RestoreMessage restoreMessage = restoreResponse.getRestoreMessage();

        PublicKey publicKey = null;
        boolean verifyTimestamp = false;
        try {
            verifyTimestamp = CryptographicOperations.verifyTimestamp(restoreMessage.getTimestamp());
            byte[] signature = Base64.getDecoder().decode(restoreResponse.getSignature());
            publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys");
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, restoreMessage.toByteArray(), signature);

            if (!verifySig || !verifyTimestamp) {
                String message = !verifySig ? "Invalid Signature" : "Invalid TimeStamp";
                logger.severe(message + " Aborting operation.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        File new_directory = new File("sync_new/");
        new_directory.mkdir();
        for (Document document : restoreMessage.getDocumentListList()) {
            DataOperations.writeFile("sync_new/" + document.getDocumentId(), document.getEncryptedDocument());
        }
        File old_directory = new File("sync/");
        DataOperations.deleteDirectory(old_directory);
        new_directory.renameTo(new File("sync/"));


        logger.info("Restore Successful.");
    }


}
