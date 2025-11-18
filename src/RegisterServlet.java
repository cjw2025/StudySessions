import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String firstName = request.getParameter("first_name");
        String lastName = request.getParameter("last_name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        
        try {
            Connection conn = DBConnection.getDBConnection();
            
            if (conn == null || conn.isClosed()) {
                response.setStatus(500);
                response.getWriter().write("{\"success\": false, \"message\": \"Cannot connect to database\"}");
                return;
            }
            
            UserDAO userDAO = new UserDAO(conn);
            
            // Check if user already exists
            User existingUser = userDAO.findByEmail(email);
            if (existingUser != null) {
                response.setStatus(400);
                response.getWriter().write("{\"success\": false, \"message\": \"Email already registered\"}");
                return;
            }
            
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhone(phone);
            
            User createdUser = userDAO.createUser(user);
            
            if (createdUser != null) {
                response.setStatus(200);
                response.getWriter().write("{\"success\": true, \"message\": \"Account created successfully!\", \"userId\": " + createdUser.getUserId() + "}");
            } else {
                response.setStatus(500);
                response.getWriter().write("{\"success\": false, \"message\": \"Registration failed\"}");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"success\": false, \"message\": \"Database error: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            DBConnection.closeConnection();
        }
    }
    
    // Helper method to escape JSON strings
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}