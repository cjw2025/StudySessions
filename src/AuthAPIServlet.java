import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * REST API Servlet for user authentication
 * No external dependencies - uses manual JSON parsing
 * 
 * Endpoints:
 * - POST /api/auth/login   - User login
 * - POST /api/auth/signup  - User registration  
 * - POST /api/auth/logout  - User logout
 */
@WebServlet(name = "AuthAPIServlet", urlPatterns = {"/api/auth/*"})
public class AuthAPIServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Set response type to JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Get the path (e.g., "/login", "/signup", "/logout")
        String pathInfo = request.getPathInfo();
        
        try {
            if ("/login".equals(pathInfo)) {
                handleLogin(request, response);
            } else if ("/signup".equals(pathInfo)) {
                handleSignup(request, response);
            } else if ("/logout".equals(pathInfo)) {
                handleLogout(request, response);
            } else {
                sendErrorResponse(response, 404, "Endpoint not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Database error occurred");
        }
    }

    /**
     * Handle login request
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        
        // Parse JSON request body
        Map<String, String> loginData = parseJsonBody(request);
        
        String email = loginData.get("email");
        String password = loginData.get("password");

        // Validate input
        if (email == null || email.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            sendErrorResponse(response, 400, "Email and password are required");
            return;
        }

        // Get database connection from servlet context
        Connection connection = (Connection) getServletContext().getAttribute("dbConnection");
        UserDAO userDAO = new UserDAO(connection);

        // Authenticate user
        User user = userDAO.authenticateUser(email.trim(), password);

        if (user != null) {
            // Create session
            HttpSession session = request.getSession();
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getUserId());
            session.setMaxInactiveInterval(3600); // 1 hour

            // Send success response
            sendSuccessResponse(response, 200, "Login successful", user);
        } else {
            sendErrorResponse(response, 401, "Invalid email or password");
        }
    }

    /**
     * Handle signup request
     */
    private void handleSignup(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        
        // Parse JSON request body
        Map<String, String> signupData = parseJsonBody(request);
        
        String firstName = signupData.get("firstName");
        String lastName = signupData.get("lastName");
        String email = signupData.get("email");
        String phone = signupData.get("phone");
        String password = signupData.get("password");

        // Validate input
        if (firstName == null || firstName.trim().isEmpty() ||
            lastName == null || lastName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            
            sendErrorResponse(response, 400, "First name, last name, email, and password are required");
            return;
        }

        // Validate password strength
        if (!PasswordUtil.isValidPassword(password)) {
            sendErrorResponse(response, 400, 
                "Password must be at least 8 characters and contain uppercase, lowercase, and a number");
            return;
        }

        // Get database connection
        Connection connection = (Connection) getServletContext().getAttribute("dbConnection");
        UserDAO userDAO = new UserDAO(connection);

        // Check if email already exists
        if (userDAO.findByEmail(email.trim()) != null) {
            sendErrorResponse(response, 409, "Email already registered");
            return;
        }

        // Create new user
        User newUser = new User(firstName.trim(), lastName.trim(), email.trim(), phone);
        User createdUser = userDAO.createUser(newUser, password);

        if (createdUser != null) {
            // Auto-login after signup
            HttpSession session = request.getSession();
            session.setAttribute("user", createdUser);
            session.setAttribute("userId", createdUser.getUserId());
            session.setMaxInactiveInterval(3600);

            // Send success response
            sendSuccessResponse(response, 201, "Account created successfully", createdUser);
        } else {
            sendErrorResponse(response, 500, "Registration failed. Please try again.");
        }
    }

    /**
     * Handle logout request
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        
        PrintWriter out = response.getWriter();
        out.print("{\"success\":true,\"message\":\"Logged out successfully\"}");
        out.flush();
    }

    /**
     * Parse JSON request body manually (no Gson needed)
     * Simple JSON parser for our specific use case
     */
    private Map<String, String> parseJsonBody(HttpServletRequest request) throws IOException {
        Map<String, String> result = new HashMap<>();
        
        // Read the request body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        
        String json = sb.toString().trim();
        
        // Remove outer braces
        if (json.startsWith("{")) {
            json = json.substring(1);
        }
        if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }
        
        // Split by comma (simple parsing - works for our use case)
        String[] pairs = json.split(",");
        
        for (String pair : pairs) {
            // Split by colon
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                // Remove quotes and whitespace
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim().replace("\"", "");
                result.put(key, value);
            }
        }
        
        return result;
    }

    /**
     * Create JSON string for user object (manual - no Gson)
     */
    private String userToJson(User user) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"userId\":").append(user.getUserId()).append(",");
        json.append("\"firstName\":\"").append(escapeJson(user.getFirstName())).append("\",");
        json.append("\"lastName\":\"").append(escapeJson(user.getLastName())).append("\",");
        json.append("\"email\":\"").append(escapeJson(user.getEmail())).append("\"");
        
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            json.append(",\"phone\":\"").append(escapeJson(user.getPhone())).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }

    /**
     * Escape special characters in JSON strings
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Send success response with user data
     */
    private void sendSuccessResponse(HttpServletResponse response, int status, String message, User user) 
            throws IOException {
        response.setStatus(status);
        PrintWriter out = response.getWriter();
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":true,");
        json.append("\"message\":\"").append(escapeJson(message)).append("\",");
        json.append("\"user\":").append(userToJson(user));
        json.append("}");
        
        out.print(json.toString());
        out.flush();
    }

    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) 
            throws IOException {
        response.setStatus(status);
        PrintWriter out = response.getWriter();
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":false,");
        json.append("\"message\":\"").append(escapeJson(message)).append("\"");
        json.append("}");
        
        out.print(json.toString());
        out.flush();
    }
}