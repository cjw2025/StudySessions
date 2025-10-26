import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnection2 {
    public static Connection connection = null;

    public static void getDBConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://ec2-3-15-177-242.us-east-2.compute.amazonaws.com:3306/myDB",
                "coryremote",
                "1337"
            );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

