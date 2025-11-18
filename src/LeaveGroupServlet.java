import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

@WebServlet("/api/groups/leave")
public class LeaveGroupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userIdStr = request.getParameter("userId");
        String groupIdStr = request.getParameter("groupId");

        if (userIdStr == null || groupIdStr == null) {
            response.setStatus(400);
            out.print("{\"success\":false,\"message\":\"Missing userId or groupId\"}");
            return;
        }

        int userId, groupId;
        try {
            userId = Integer.parseInt(userIdStr);
            groupId = Integer.parseInt(groupIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(400);
            out.print("{\"success\":false,\"message\":\"Invalid ID format\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DBConnection.getDBConnection();

            String sql = "DELETE FROM Study_group_membership WHERE user_id = ? AND group_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, groupId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                out.print("{\"success\":true,\"message\":\"Successfully left the group\"}");
            } else {
                out.print("{\"success\":false,\"message\":\"You are not a member of this group\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"success\":false,\"message\":\"Database error\"}");
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
            DBConnection.closeConnection();
        }
    }
}