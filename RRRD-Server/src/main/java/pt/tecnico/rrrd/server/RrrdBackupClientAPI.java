package pt.tecnico.rrrd.server;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerBlockingStub;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerStub;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.crypto.CryptographicOperations;
import pt.tecnico.rrrd.crypto.DataOperations;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;
import pt.tecnico.rrrd.server.utils.Utils;

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
        File dir = new File(Utils.getServerSyncDirectory());
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

        Document dbBackup = Document.newBuilder().setDocumentId("backup.sql").setEncryptedDocument(dumpDatabase()).build();


        updateMessageBuilder.setTimestamp(CryptographicOperations.getTimestamp());
        updateMessageBuilder.setDbBackup(dbBackup);
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
            publicKey = CryptographicOperations.getPublicKey("password", "backup");
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

        File directory = new File(Utils.getServerSyncDirectory());
        directory.mkdir();
        DataOperations.deleteTxtDirectory(directory);
        for (Document document : restoreMessage.getDocumentListList()) {
            DataOperations.writeFile(Utils.getServerSyncDirectory()+ document.getDocumentId(), document.getEncryptedDocument());
        }
        File backup_directory = new File(Utils.getServerBackupDirectory());
        DataOperations.deleteDirectory(backup_directory);
        backup_directory.mkdir();
        DataOperations.writeFile(Utils.getServerBackupDirectory() + restoreMessage.getDbBackup().getDocumentId(), restoreMessage.getDbBackup().getEncryptedDocument());

        restoreDatabase();


        logger.info("Restore Successful.");
    }

    public static String dumpDatabase() {
        String result = null;
        try {
            System.out.println("Database Backup Started");

            Properties prop = new Properties();
            prop.load(new FileInputStream("src/main/java/pt/tecnico/rrrd/server/database/database.properties"));
            String[] dbUrl = prop.getProperty("dbUrl").split("/");
            String dbname = dbUrl[dbUrl.length - 1];
            String dbUser = prop.getProperty("dbUser");
            String dbPassword = prop.getProperty("dbPassword");

            /*NOTE: Creating Path Constraints for folder saving*/
            /*NOTE: Here the backup folder is created for saving inside it*/
            String folderPath = Utils.getServerBackupDirectory();

            /*NOTE: Creating Folder if it does not exist*/
            File f1 = new File(folderPath);
            f1.mkdirs();
            String savePath = folderPath + "backup.sql";
            /*NOTE: Creating Path Constraints for backup saving*/
            /*NOTE: Here the backup is saved in a folder called backup with the name backup.sql*/

            /*NOTE: Used to create a cmd command*/
            String executeCmd = "mysqldump -u" + dbUser + " -p" + dbPassword + " --databases " + dbname + " -r " + savePath;

            /*NOTE: Executing the command here*/
            Process runtimeProcess = Runtime.getRuntime().exec(executeCmd);
            int processComplete = runtimeProcess.waitFor();

            /*NOTE: processComplete=0 if correctly executed, will contain other values if not*/
            if (processComplete == 0) {
                System.out.println("Database Backup Complete");
            } else {
                System.out.println("Database Backup Failure");
            }

            result = Files.readString(Path.of(savePath), StandardCharsets.ISO_8859_1);
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
        return result;

    }

    public static void restoreDatabase() {
        try {
            System.out.println("Database Restore Started");

            Properties prop = new Properties();
            prop.load(new FileInputStream("src/main/java/pt/tecnico/rrrd/server/database/database.properties"));
            String[] dbUrl = prop.getProperty("dbUrl").split("/");
            String dbname = dbUrl[dbUrl.length - 1];
            String dbUser = prop.getProperty("dbUser");
            String dbPassword = prop.getProperty("dbPassword");

            String restorePath = Utils.getServerBackupDirectory() + "backup.sql";

            /*NOTE: Used to create a cmd command*/
            /*NOTE: Do not create a single large string, this will cause buffer locking, use string array*/
//            String[] executeCmd = new String[]{"mysql", dbname, "-u" + dbUser, "-p" + dbPassword, "-e", " source " + restorePath};
            String executeCmd = "mysql -u" + dbUser + " -p" + dbPassword + " " + dbname + " -e \"source " + restorePath + "\"";
            /*NOTE: processComplete=0 if correctly executed, will contain other values if not*/
            Process runtimeProcess = Runtime.getRuntime().exec(executeCmd);
            int processComplete = runtimeProcess.waitFor();

            /*NOTE: processComplete=0 if correctly executed, will contain other values if not*/
            if (processComplete == 0) {
                System.out.println("Database Restore Complete");
            } else {
                System.out.println("Database Restore Failed");
            }


        } catch (IOException | InterruptedException | HeadlessException ex) {
            ex.printStackTrace();
        }

    }


}
