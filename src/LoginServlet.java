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
        // String password = request.getParameter("password");  // ignored for now

        if (email == null || email.trim().isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"message\": \"Email is required\"}");
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
                // User found → create session
                HttpSession session = request.getSession(true); // true = create if not exists
                session.setMaxInactiveInterval(30 * 60); // 30 minutes

                session.setAttribute("userId", rs.getInt("UserID"));
                session.setAttribute("firstName", rs.getString("First_name"));
                session.setAttribute("lastName", rs.getString("Last_name"));
                session.setAttribute("email", rs.getString("Email"));

                // Send back user info (for localStorage if you want)
                String jsonResponse = String.format(
                    "{\"success\": true, \"user\": {\"id\": %d, \"firstName\": \"%s\", \"email\": \"%s\"}}",
                    rs.getInt("UserID"),
                    rs.getString("First_name"),
                    rs.getString("Email")
                );

                response.setStatus(200);
                response.getWriter().write(jsonResponse);

            } else {
                response.setStatus(401);
                response.getWriter().write("{\"message\": \"Email not found. Try cory@example.com or add yourself to the DB.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"message\": \"Server error. Check console.\"}");
        } finally {
            DBConnection.closeConnection();
        }
    }
}