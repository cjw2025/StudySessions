import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    private Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    // Create a new user
    public User createUser(User user, String plainPassword) throws SQLException {
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);
        String sql = "INSERT INTO `User` (First_name, Last_name, Email, Phone, Password_hash) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPhone());
            stmt.setString(5, hashedPassword);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) return null;

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                    user.setPasswordHash(hashedPassword);
                    return user;
                }
            }
        }

        return null;
    }

    // Authenticate user
    public User authenticateUser(String email, String plainPassword) throws SQLException {
        String sql = "SELECT * FROM `User` WHERE Email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("Password_hash");
                    if (PasswordUtil.verifyPassword(plainPassword, storedHash)) {
                        User user = new User();
                        user.setUserId(rs.getInt("UserID"));
                        user.setFirstName(rs.getString("First_name"));
                        user.setLastName(rs.getString("Last_name"));
                        user.setEmail(rs.getString("Email"));
                        user.setPhone(rs.getString("Phone"));
                        user.setPasswordHash(storedHash);
                        return user;
                    }
                }
            }
        }
        return null;
    }
    
 // find by email
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `User` WHERE Email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("UserID"));
                    user.setFirstName(rs.getString("First_name"));
                    user.setLastName(rs.getString("Last_name"));
                    user.setEmail(rs.getString("Email"));
                    user.setPhone(rs.getString("Phone"));
                    user.setPasswordHash(rs.getString("Password_hash"));
                    return user;
                }
            }
        }

        return null; // Not found
    }

}
