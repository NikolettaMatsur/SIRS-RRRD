package pt.tecnico.rrrd.server.utils;

public class Utils {

    public static String getUserName() {
        return System.getProperty("user.name");
    }

    public static String getFileRepository(String documentId) {
        return "/home/" + getUserName() + "/sync/server/" + documentId;
    }
}
