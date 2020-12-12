package pt.tecnico.rrrd.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private String jdbcDriver;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public DatabaseManager() throws ClassNotFoundException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("src/main/java/pt/tecnico/rrrd/server/database/database.properties"));
        jdbcDriver = prop.getProperty("jdbcDriver");
        dbUrl = prop.getProperty("dbUrl");
        dbUser = prop.getProperty("dbUser");
        dbPassword = prop.getProperty("dbPassword");
        Class.forName(jdbcDriver);
    }

    /*--------------------------------------------USER---------------------------------------------*/

    public void insertUser(String username, byte[] password, byte[] salt) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Inserting user " + username );

        String query = " INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setBytes(2, password);
        stmt.setBytes(3, salt);

        stmt.execute();

        stmt.close();
        conn.close();
    }

    public void updateUserPassword(String username, byte[] password, byte[] salt) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Updating user " + username );

        String query = "UPDATE users SET password = ?, salt = ? WHERE username = ?";

        stmt = conn.prepareStatement(query);
        stmt.setBytes(1, password);
        stmt.setBytes(2, salt);
        stmt.setString(3, username);

        stmt.execute();

        stmt.close();
        conn.close();
    }

    public void deleteUser(String username) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Removing user " + username );

        //delete user means to delete their user, their permissions, and files that dont have more permission
        String permissions = "DELETE FROM permissions WHERE username=?";
        stmt = conn.prepareStatement(permissions);
        stmt.setString(1, username);
        stmt.executeUpdate();

        String users = "DELETE FROM users WHERE username=?";
        stmt = conn.prepareStatement(users);
        stmt.setString(1, username);
        stmt.executeUpdate();

        String files = "UPDATE files SET owner=NULL WHERE owner=? ";
        stmt = conn.prepareStatement(files);
        stmt.setString(1, username);
        stmt.executeUpdate();
        
        //TODO file is never deleted, it should be if it has no permissions -> natural join
        stmt.close();
        conn.close();
    }

    public byte[] getSalt(String username) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rst = null;
        Connection conn = null;
        byte[] result = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = " SELECT salt FROM users WHERE username=?";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);

        rst = stmt.executeQuery();

        if (rst.next())
            result = rst.getBytes("salt");
        //there is always 1 because username is unique as it is primary key
        rst.close();
        stmt.close();
        conn.close();
        return result;
    }


    //to use when altering password through grpc
    public boolean verifyUserPassword(String username, byte[] password) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rst = null;
        Connection conn = null;
        boolean result = false;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = " SELECT password FROM users WHERE username=?";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);

        rst = stmt.executeQuery();

        if (rst.next())
            result = Arrays.equals(rst.getBytes(1), password);
            //there is always 1 because username is unique as it is primary key
        rst.close();
        stmt.close();
        conn.close();
        return result;
    }

    /*----------------------------------------PUBLIC KEYS-----------------------------------------------------*/
    public void insertPubKey(String username, String pubKey) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Inserting pubKey to user " +  username);

        String query = "INSERT INTO public_keys(username, pub_key) VALUES(?,?)";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setString(2, pubKey);

        stmt.execute();

        stmt.close();
        conn.close();
    }

    public void deletePubKey(String username, String pubKey) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Deleting pubKey of user " +  username);

        String query = "DELETE FROM public_keys WHERE username=? AND pub_key=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setString(2, pubKey);

        stmt.execute();

        stmt.close();
        conn.close();
    }

    public void deletePubKey(String username, int entry_id) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Deleting pubKey of user " +  username);

        String query = "DELETE FROM public_keys WHERE username=? AND id=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setInt(2, entry_id);

        stmt.execute();

        stmt.close();
        conn.close();
    }

    public Map<Integer,String> getPubKeys(String username) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet rst = null;
        Integer id;
        String pubKey;
        Map<Integer,String> pubKeys = new HashMap<>();

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Getting pubKeys of user " +  username);

        String query = "SELECT id,pub_key FROM public_keys WHERE username=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);

        rst = stmt.executeQuery();

        while (rst.next()) {
            id = rst.getInt("id");
            pubKey=rst.getString("pub_key");
            pubKeys.put(id, pubKey);
        }

        stmt.close();
        conn.close();

        return pubKeys;
    }

    public Integer getPubKeyId(String username, String pubKey) throws SQLException{
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet rst = null;
        Integer result = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Getting pubKey of user " +  username);

        String query = "SELECT id FROM public_keys WHERE username=? AND pub_key=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setString(2,pubKey);

        rst = stmt.executeQuery();

        if (rst.next())
            result = rst.getInt("id");

        stmt.close();
        conn.close();
        return result;

    }

    public String getPubKey(String username, Integer pub_key_id) throws SQLException{
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet rst = null;
        String result = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Getting pubKey of user " +  username);

        String query = "SELECT pub_key FROM public_keys WHERE username=? AND id=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setInt(2,pub_key_id);

        rst = stmt.executeQuery();

        if (rst.next())
            result = rst.getString("pub_key");

        stmt.close();
        conn.close();
        return result;

    }

    /*----------------------------------------FILES-----------------------------------------------------------*/

    public void insertFile(String filename, String owner) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Inserting file " + filename );

        String query = "INSERT INTO files(filename, owner) VALUES(?,?)";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, filename);
        stmt.setString(2, owner);

        stmt.execute();

        stmt.close();
        conn.close();
    }

    public void deleteFile(String filename) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Removing file " + filename );

        String files = "DELETE FROM files WHERE filename=?";

        stmt = conn.prepareStatement(files);
        stmt.setString(1, filename);

        stmt.execute();

        String permissions = "DELETE FROM permissions WHERE filename=?";

        stmt = conn.prepareStatement(permissions);
        stmt.setString(1, filename);
        stmt.execute();

        stmt.close();
        conn.close();
    }

    public boolean verifyFileExists(String filename) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet rst = null;
        boolean result = false;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Verifiyng file existence " +  filename);

        String query = "SELECT * FROM files WHERE filename=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, filename);

        rst = stmt.executeQuery();

        if(rst.next()) {
            result = true;
        }

        stmt.close();
        conn.close();

        return result;
    }

    public List<String> getAllAllowedFiles(String username) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet rst = null;
        String pubKey;
        List<String> allowedFiles = new ArrayList<>();

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Getting pubKeys of user " +  username);

        String query = "SELECT filename FROM files WHERE username=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);

        rst = stmt.executeQuery();

        while (rst.next()) {
            pubKey = rst.getString("filename");
            allowedFiles.add(pubKey);
        }

        stmt.close();
        conn.close();

        return allowedFiles;
    }

    public boolean verifyOwner(String filename, String username) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet rst = null;
        String pubKey;
        boolean result = false;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Verifiyng ownership of user " +  username);

        String query = "SELECT * FROM files WHERE owner=? AND filename=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setString(2, filename);

        rst = stmt.executeQuery();

       if(rst.next()) {
            result = true;
        }

        stmt.close();
        conn.close();

        return result;
    }



    /*----------------------------------------PERMISSIONS-----------------------------------------------------*/

    public void insertPermission(String filename, String username, Integer pub_key_id, String permission_key) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Inserting permission to" + username + " file: " + filename);

        String query = "INSERT INTO permissions(filename,username,pub_key_id,permission_key) VALUES(?,?,?,?)";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, filename);
        stmt.setString(2, username);
        stmt.setInt(3, pub_key_id);
        stmt.setString(4, permission_key);
        stmt.execute();

        stmt.close();
        conn.close();
    }

    public void deletePermission(String filename, String username) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Deleting permission of" + username + " to file " + filename);

        String query = "DELETE FROM permissions WHERE username=? AND filename=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setString(2, filename);
        stmt.execute();

        stmt.close();
        conn.close();
    }


    public boolean verifyPermission(String filename, String username, Integer pub_key_id) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        boolean result = false;
        ResultSet rst = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Verifiying permission to" + username + " file " + filename);

        String query = "SELECT permission_key FROM permissions WHERE filename=? AND username=? AND pub_key_id=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, filename);
        stmt.setString(2, username);
        stmt.setInt(3, pub_key_id);
        rst = stmt.executeQuery();

        if(rst.next()){
            result = true;
        }

        stmt.close();
        conn.close();
        return result;
    }

    public String getPermissionKey(String filename, String username, Integer pub_key_id) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        String result = null;
        ResultSet rst = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Getting permission to" + username + " file " + filename);

        String query = "SELECT permission_key FROM permissions WHERE filename=? AND username=? AND pub_key_id=?";

        stmt = conn.prepareStatement(query);
        stmt.setString(1, filename);
        stmt.setString(2, username);
        stmt.setInt(3, pub_key_id);
        rst = stmt.executeQuery();

        if(rst.next()){
            result = rst.getString("permission_key");
        }

        stmt.close();
        conn.close();
        return result;
    }
}
