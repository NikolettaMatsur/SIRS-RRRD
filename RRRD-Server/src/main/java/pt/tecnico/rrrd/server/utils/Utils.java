package pt.tecnico.rrrd.server.utils;

public class Utils {
        static String serverRootDirectory = "C:/sync/";
//    static String serverRootDirectory = "/home/" + getUserName();
    static String serverSyncDirectory = serverRootDirectory + "server/sync/";
    static String serverBackupDirectory = serverRootDirectory + "server/serverDbBackup/";


    public static String getUserName() {
        return System.getProperty("user.name");
    }

    public static String getFileRepository(String documentId) {
        return serverSyncDirectory + documentId + ".txt";
    }

    public static String getServerSyncDirectory(){
        return serverSyncDirectory;
    }

    public static String getServerBackupDirectory() {
        return serverBackupDirectory;
    }
}
