import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object for User operations
 * Handles all database operations for users (CRUD operations)
 * 
 * Methods:
 * - createUser()        - Insert new user into database
 * - authenticateUser()  - Verify email/password and return user
 * - findByEmail()       - Find user by email address
 * - findById()          - Find user by ID
 * - updatePassword()    - Change user's password
 * - deleteUser()        - Remove user from database
 */
public class UserDAO {
    private Connection connection;

    /**
     * Constructor - requires a database connection
     * @param connection Active database connection
     */
    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Create a new user account in the database
     * @param user User object with details (firstName, lastName, email, phone)
     * @param plainTextPassword User's password (will be hashed before storing)
     * @return The created user with ID set, or null if failed
     */
    public User createUser(User user, String plainTextPassword) throws SQLException {
        String sql = "INSERT INTO User (First_name, Last_name, Email, Phone, Password_hash) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPhone());
            stmt.setString(5, PasswordUtil.hashPassword(plainTextPassword));  // Hash the password!
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                return null;
            }
            
            // Get the auto-generated UserID
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                    return user;
                }
            }
        }
        
        return null;
    }

    /**
     * Authenticate a user with email and password
     * @param email User's email
     * @param plainTextPassword User's password
     * @return User object if authenticated, null otherwise
     */
    public User authenticateUser(String email, String plainTextPassword) throws SQLException {
        String sql = "SELECT * FROM User WHERE Email = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("Password_hash");
                    
                    // Verify password using BCrypt
                    if (PasswordUtil.verifyPassword(plainTextPassword, storedHash)) {
                        User user = extractUserFromResultSet(rs);
                        
                        // Update last login time
                        updateLastLogin(user.getUserId());
                        
                        return user;
                    }
                }
            }
        }
        
        return null;  // Email not found or password incorrect
    }

    /**
     * Find a user by email address
     * @param email User's email
     * @return User object or null if not found
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM User WHERE Email = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        }
        
        return null;
    }

    /**
     * Find a user by UserID
     * @param userId User's ID
     * @return User object or null if not found
     */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM User WHERE UserID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        }
        
        return null;
    }

    /**
     * Update user's password
     * @param userId User's ID
     * @param newPlainTextPassword New password (will be hashed)
     * @return true if successful
     */
    public boolean updatePassword(int userId, String newPlainTextPassword) throws SQLException {
        String sql = "UPDATE User SET Password_hash = ? WHERE UserID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, PasswordUtil.hashPassword(newPlainTextPassword));
            stmt.setInt(2, userId);
            
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Update last login timestamp for a user
     * Called automatically after successful authentication
     */
    private void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE User SET Last_login = CURRENT_TIMESTAMP WHERE UserID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Extract User object from ResultSet
     * Converts database row into User object
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("UserID"));
        user.setFirstName(rs.getString("First_name"));
        user.setLastName(rs.getString("Last_name"));
        user.setEmail(rs.getString("Email"));
        user.setPhone(rs.getString("Phone"));
        user.setPasswordHash(rs.getString("Password_hash"));
       
        return user;
    }

    /**
     * Delete a user account
     * WARNING: This will cascade delete related records (study groups, etc.)
     * @param userId User's ID
     * @return true if successful
     */
    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM User WHERE UserID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }
}