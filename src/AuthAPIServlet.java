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

@WebServlet(name = "AuthAPIServlet", urlPatterns = {"/api/auth/*"})
public class AuthAPIServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

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

    // -----------------------------
    // Handle login
    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        Map<String, String> loginData = parseJsonBody(request);
        String email = loginData.get("email");
        String password = loginData.get("password");

        if (email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            sendErrorResponse(response, 400, "Email and password are required");
            return;
        }

        Connection connection = DBConnection.getDBConnection();
        UserDAO userDAO = new UserDAO(connection);

        User user = userDAO.authenticateUser(email.trim(), password);

        if (user != null) {
            HttpSession session = request.getSession();
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getUserId());
            session.setMaxInactiveInterval(3600);

            sendSuccessResponse(response, 200, "Login successful", user);
        } else {
            sendErrorResponse(response, 401, "Invalid email or password");
        }
    }

    // -----------------------------
    // Handle signup
    private void handleSignup(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        Map<String, String> signupData = parseJsonBody(request);

        String firstName = signupData.get("firstName");
        String lastName = signupData.get("lastName");
        String email = signupData.get("email");
        String phone = signupData.get("phone");
        String password = signupData.get("password");

        if (firstName == null || firstName.trim().isEmpty() ||
            lastName == null || lastName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            sendErrorResponse(response, 400, "First name, last name, email, and password are required");
            return;
        }

        if (!PasswordUtil.isValidPassword(password)) {
            sendErrorResponse(response, 400,
                    "Password must be at least 8 characters and contain uppercase, lowercase, and a number");
            return;
        }

        Connection connection = DBConnection.getDBConnection();
        UserDAO userDAO = new UserDAO(connection);

        if (userDAO.findByEmail(email.trim()) != null) {
            sendErrorResponse(response, 409, "Email already registered");
            return;
        }

        User newUser = new User(firstName.trim(), lastName.trim(), email.trim(), phone);
        User createdUser = userDAO.createUser(newUser, password);

        if (createdUser != null) {
            HttpSession session = request.getSession();
            session.setAttribute("user", createdUser);
            session.setAttribute("userId", createdUser.getUserId());
            session.setMaxInactiveInterval(3600);

            sendSuccessResponse(response, 201, "Account created successfully", createdUser);
        } else {
            sendErrorResponse(response, 500, "Registration failed. Please try again.");
        }
    }

    // -----------------------------
    // Handle logout
    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();

        PrintWriter out = response.getWriter();
        out.print("{\"success\":true,\"message\":\"Logged out successfully\"}");
        out.flush();
    }

    // -----------------------------
    // Simple manual JSON parser
    private Map<String, String> parseJsonBody(HttpServletRequest request) throws IOException {
        Map<String, String> result = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        String json = sb.toString().trim();

        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim().replace("\"", "");
                result.put(key, value);
            }
        }
        return result;
    }

    // -----------------------------
    // Convert user to JSON
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

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private void sendSuccessResponse(HttpServletResponse response, int status, String message, User user)
            throws IOException {
        response.setStatus(status);
        PrintWriter out = response.getWriter();
        out.print("{\"success\":true,\"message\":\"" + escapeJson(message) + "\",\"user\":" + userToJson(user) + "}");
        out.flush();
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        PrintWriter out = response.getWriter();
        out.print("{\"success\":false,\"message\":\"" + escapeJson(message) + "\"}");
        out.flush();
    }
}
