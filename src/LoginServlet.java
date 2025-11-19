
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");

        if (email == null || email.trim().isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"success\":false,\"message\": \"Email is required\"}");
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getDBConnection();

            String sql = "SELECT UserID, First_name, Last_name, Email FROM User WHERE Email = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("UserID");
                String firstName = rs.getString("First_name");
                String lastName = rs.getString("Last_name");
                String userEmail = rs.getString("Email");

                // Store in session
                HttpSession session = request.getSession(true);
                session.setMaxInactiveInterval(30 * 60);
                session.setAttribute("userId", userId);
                session.setAttribute("firstName", firstName);
                session.setAttribute("lastName", lastName);
                session.setAttribute("email", userEmail);

                // SEND lastName TO FRONTEND TOO!
                String jsonResponse = String.format(
                    "{\"success\": true, \"user\": {\"id\": %d, \"firstName\": \"%s\", \"lastName\": \"%s\", \"email\": \"%s\"}}",
                    userId,
                    escape(firstName),
                    escape(lastName),
                    escape(userEmail)
                );

                response.setStatus(200);
                response.getWriter().write(jsonResponse);

            } else {
                response.setStatus(401);
                response.getWriter().write("{\"success\":false,\"message\": \"Email not found. Try cory@example.com or add yourself to the DB.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"success\":false,\"message\": \"Server error. Check console.\"}");
        } finally {
            DBConnection.closeConnection();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}