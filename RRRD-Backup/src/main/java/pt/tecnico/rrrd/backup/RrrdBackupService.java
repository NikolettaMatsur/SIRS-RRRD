package pt.tecnico.rrrd.backup;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.crypto.CryptographicOperations;
import pt.tecnico.rrrd.crypto.DataOperations;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;

public class RrrdBackupService extends BackupServerGrpc.BackupServerImplBase {

    private final String currentVersionPath = "currentVersion.txt";
    private final Logger logger;
    private int MAX_VERSIONS = 1;
    private static String keyStorePassword;
    static String backupSaveDirectory = "/home/" + System.getProperty("user.name") + "/backup/";


    public RrrdBackupService(String keyStorePassword) {
        this.logger = Logger.getLogger(RrrdBackupService.class.getName());
        this.keyStorePassword = keyStorePassword;
        this.MAX_VERSIONS = RrrdBackupApp.max_versions;
    }


    @Override
    public void update(UpdateRequest request, StreamObserver<UpdateResponse> responseObserver) {
        System.out.println("Received update request.");
        PublicKey publicKey = null;
        UpdateMessage updateMessage = request.getUpdateMessage();
        try {
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(updateMessage.getTimestamp());
            byte[] signature = Base64.getDecoder().decode(request.getSignature());
            publicKey = CryptographicOperations.getPublicKey("password", "remote");
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, updateMessage.toByteArray(), signature);

            if (!verifySig || !verifyTimestamp) {
                String message = !verifySig ? "Invalid Signature" : "Invalid TimeStamp";
                logger.severe(message + " Aborting operation.");
                responseObserver.onError(Status.DATA_LOSS
                        .withDescription(message)
                        .asRuntimeException());
                responseObserver.onCompleted();
                return;
            }
        } catch (Exception e) {

            responseObserver.onError(Status.DATA_LOSS
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
            responseObserver.onCompleted();
            return;
        }

        int version = getCurrentVersion() + 1;
        updateCurrentVersion(version);

        if (version > MAX_VERSIONS) {
            File oldDirectory = new File(backupSaveDirectory + "versions/" + (version - MAX_VERSIONS) + "/");
            if (oldDirectory.exists()) {
                DataOperations.deleteDirectory(oldDirectory);
                File metaData = new File(backupSaveDirectory + "versions/" + (version - MAX_VERSIONS) + ".txt");
                metaData.delete();
                File dbBackup = new File(backupSaveDirectory + "versions/" + (version - MAX_VERSIONS) + ".sql");
                dbBackup.delete();
            }
        }

        File directory = new File(backupSaveDirectory + "versions/" + version + "/");
        directory.mkdirs();
        for (Document document : updateMessage.getDocumentListList()) {
            DataOperations.writeFile(backupSaveDirectory + "versions/" + version + "/" + document.getDocumentId(), document.getEncryptedDocument());
        }

        String metaData = updateMessage.getTimestamp() + "\n" + updateMessage.getDocumentListCount();
        DataOperations.writeFile(backupSaveDirectory + "versions/" + version + ".txt", metaData);

        String dbBackup = updateMessage.getDbBackup().getEncryptedDocument();
        DataOperations.writeFile(backupSaveDirectory + "versions/" + version + ".sql", dbBackup);

        UpdateResponse updateResponse = UpdateResponse.newBuilder().setStatus("OK").setSignature("0").build();

        responseObserver.onNext(updateResponse);
        responseObserver.onCompleted();

    }

    @Override
    public void restore(RestoreRequest request, StreamObserver<RestoreResponse> responseObserver) {
        RestoreMessage.Builder restoreMessageBuilder = RestoreMessage.newBuilder();
        int currentVersion = getCurrentVersion();
        int requestedVersion = request.getVersion();
        if (requestedVersion <= currentVersion - MAX_VERSIONS || requestedVersion > currentVersion) {
            String message = "Version not found. Aborting.";
            logger.severe(message);
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(message)
                    .asRuntimeException());
            responseObserver.onCompleted();
            return;
        }

        File dir = new File(backupSaveDirectory + "versions/" + requestedVersion + "/");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            this.logger.info(String.format("Sending new restore response for %d files.", directoryListing.length));
            for (File child : directoryListing) {
                String data = String.join("\n", DataOperations.readFile(child.getPath()));
                Document document = Document.newBuilder().setDocumentId(child.getName()).setEncryptedDocument(data).build();
                restoreMessageBuilder.addDocumentList(document);
            }
        }
        String dbBackupString = null;
        try {
            dbBackupString = Files.readString(Path.of(backupSaveDirectory + "versions/" + requestedVersion + ".sql"), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Document dbBackup = Document.newBuilder().setDocumentId("backup.sql").setEncryptedDocument(dbBackupString).build();

        restoreMessageBuilder.setDbBackup(dbBackup);
        restoreMessageBuilder.setTimestamp(CryptographicOperations.getTimestamp());
        RestoreMessage restoreMessage = restoreMessageBuilder.build();

        RestoreResponse restoreResponse = null;
        try {
            restoreResponse = RestoreResponse.newBuilder()
                    .setRestoreMessage(restoreMessageBuilder.build())
                    .setSignature(CryptographicOperations.getSignature(this.keyStorePassword, "asymmetric_keys", restoreMessage.toByteArray()))
                    .build();
        } catch (Exception e) {

            responseObserver.onError(Status.DATA_LOSS
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onNext(restoreResponse);
        responseObserver.onCompleted();
    }


    @Override
    public void getVersions(GetVersionsRequest request, StreamObserver<GetVersionsResponse> responseObserver) {
        //this.logger.info(String.format("Sending new update request for %d files.", directoryListing.length));
        GetVersionsResponse.Builder getVersionResponseBuilder = GetVersionsResponse.newBuilder();

        File dir = new File(backupSaveDirectory + "versions");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (!child.isDirectory() && child.getName().endsWith(".txt")) {
                    String[] metadata = DataOperations.readFile(child.getPath());
                    System.out.println(Arrays.toString(metadata));
                    Version version = Version.newBuilder()
                            .setDate(metadata[0])
                            .setNumberOfFiles(Integer.parseInt(metadata[1]))
                            .setNumber(Integer.parseInt(child.getName().split(".txt")[0]))
                            .build();
                    getVersionResponseBuilder.addVersionList(version);
                }
            }
        }

        responseObserver.onNext(getVersionResponseBuilder.build());
        responseObserver.onCompleted();
    }

    public int getCurrentVersion() {
        return Integer.parseInt(DataOperations.readFile(currentVersionPath)[0]);
    }

    public void updateCurrentVersion(int version) {
        DataOperations.writeFile(currentVersionPath, String.valueOf(version));
    }
}

