import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification using SHA-256 with salt
 * 
 * NOTE: This uses MessageDigest (SHA-256) which is less secure than BCrypt,
 * but doesn't require external libraries.
 * 
 * For production apps, BCrypt is recommended!
 * 
 * NEVER store plain text passwords in your database!
 */
public class PasswordUtil {
    
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    /**
     * Hash a password using SHA-256 with a random salt
     * The returned string contains both the salt and hash
     * 
     * Format: base64(salt):base64(hash)
     * Example: "abc123def456:xyz789ghi012"
     * 
     * @param plainTextPassword The password to hash
     * @return The hashed password with salt (safe to store in database)
     */
    public static String hashPassword(String plainTextPassword) {
        try {
            // Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            // Hash the password with the salt
            byte[] hash = hashWithSalt(plainTextPassword, salt);
            
            // Combine salt and hash, encoded as base64
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);
            
            // Return in format: salt:hash
            return saltBase64 + ":" + hashBase64;
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify a password against a stored hash
     * Use this during login to check if password is correct
     * 
     * @param plainTextPassword The password to verify (from user input)
     * @param storedPassword The stored hash to check against (from database)
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainTextPassword, String storedPassword) {
        try {
            // Split the stored password into salt and hash
            String[] parts = storedPassword.split(":");
            if (parts.length != 2) {
                return false;  // Invalid format
            }
            
            // Decode the salt and hash from base64
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            
            // Hash the input password with the same salt
            byte[] actualHash = hashWithSalt(plainTextPassword, salt);
            
            // Compare the hashes (constant-time comparison to prevent timing attacks)
            return MessageDigest.isEqual(expectedHash, actualHash);
            
        } catch (Exception e) {
            return false;  // Invalid format or other error
        }
    }

    /**
     * Hash a password with a given salt
     * Internal helper method
     */
    private static byte[] hashWithSalt(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        
        // Add salt to the digest
        md.update(salt);
        
        // Hash the password
        return md.digest(password.getBytes());
    }

    /**
     * Check if a password meets minimum security requirements
     * 
     * Requirements:
     * - At least 8 characters long
     * - Contains at least one uppercase letter (A-Z)
     * - Contains at least one lowercase letter (a-z)
     * - Contains at least one digit (0-9)
     * 
     * @param password The password to validate
     * @return true if password meets requirements, false otherwise
     */
    public static boolean isValidPassword(String password) {
        // Check minimum length
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Check for required character types
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        
        // Must have all three types
        return hasUpper && hasLower && hasDigit;
    }

    /**
     * Optional: Check password strength and return a score
     * 
     * @param password The password to check
     * @return Strength score: 0=very weak, 1=weak, 2=fair, 3=strong, 4=very strong
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        
        // Length bonus
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        
        // Character variety bonus
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        
        int varietyCount = 0;
        if (hasUpper) varietyCount++;
        if (hasLower) varietyCount++;
        if (hasDigit) varietyCount++;
        if (hasSpecial) varietyCount++;
        
        if (varietyCount >= 3) score++;
        if (varietyCount == 4) score++;
        
        return Math.min(score, 4);  // Cap at 4
    }
}