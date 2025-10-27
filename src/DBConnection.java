import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
   static Connection connection = null;
   
   // Make this public so other classes can use it
   public static Connection getDBConnection() throws SQLException {
      System.out.println("-------- MySQL JDBC Connection Testing ------------");
      
      // Only create new connection if needed
      if (connection != null && !connection.isClosed()) {
         return connection;
      }
      
      try {
         // Use newer driver class (backwards compatible)
         Class.forName("com.mysql.cj.jdbc.Driver");
      } catch (ClassNotFoundException e) {
         // Fallback to old driver
         try {
            Class.forName("com.mysql.jdbc.Driver");
         } catch (ClassNotFoundException ex) {
            System.out.println("Where is your MySQL JDBC Driver?");
            e.printStackTrace();
            throw new SQLException("JDBC Driver not found", e);
         }
      }
      
      System.out.println("MySQL JDBC Driver Registered!");
      
      try {
         UtilProp.loadProperty();
         String url = getURL();
         String user = getUserName();
         String password = getPassword();
         
         connection = DriverManager.getConnection(url, user, password);
      } catch (Exception e) {
         System.out.println("Connection Failed! Check output console");
         e.printStackTrace();
         throw new SQLException("Failed to establish connection", e);
      }
      
      if (connection != null) {
         System.out.println("You made it, take control your database now!");
      } else {
         System.out.println("Failed to make connection!");
         throw new SQLException("Failed to make connection");
      }
      
      return connection;
   }
   
   static String getURL() {
      String url = UtilProp.getProp("url");
      System.out.println("[DBG] URL: " + url);
      return url;
   }
   
   static String getUserName() {
      String usr = UtilProp.getProp("user");
      System.out.println("[DBG] User: " + usr);
      return usr;
   }
   
   static String getPassword() {
      String pwd = UtilProp.getProp("password");
      System.out.println("[DBG] Password: " + (pwd != null ? "***" : "null"));
      return pwd;
   }
   
   // Add this method for cleanup
   public static void closeConnection() {
      if (connection != null) {
         try {
            connection.close();
            System.out.println("Database connection closed");
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }
}