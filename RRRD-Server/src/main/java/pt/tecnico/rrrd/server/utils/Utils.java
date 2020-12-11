package pt.tecnico.rrrd.server.utils;

public class Utils {

    static String serverRootDirectory = "/home/" + getUserName() + "/sync/server/";
//    static String serverRootDirectory = "C:/sync/server/";

    public static String getUserName() {
        return System.getProperty("user.name");
    }

    public static String getFileRepository(String documentId) {
        return serverRootDirectory + documentId + ".txt";
    }
}
