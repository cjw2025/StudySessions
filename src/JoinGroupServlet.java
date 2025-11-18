import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/api/groups/join")
public class JoinGroupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Get logged-in user from request
        String userJson = request.getParameter("user");
        if (userJson == null || userJson.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("{\"success\": false, \"message\": \"Not logged in\"}");
            return;
        }

        // Parse user ID from JSON
        int userId;
        try {
            // Try to find "id" first, then "UserID" as fallback
            String idStr;
            if (userJson.contains("\"id\":")) {
                idStr = userJson.split("\"id\":")[1].split(",")[0].trim();
            } else if (userJson.contains("\"UserID\":")) {
                idStr = userJson.split("\"UserID\":")[1].split(",")[0].trim();
            } else {
                throw new Exception("User ID not found in JSON");
            }
            userId = Integer.parseInt(idStr);
        } catch (Exception e) {
            response.setStatus(400);
            response.getWriter().write("{\"success\": false, \"message\": \"Invalid user data: " + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        // Get group ID from request
        String groupIdParam = request.getParameter("groupId");
        if (groupIdParam == null || groupIdParam.trim().isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"success\": false, \"message\": \"Group ID is required\"}");
            return;
        }

        int groupId;
        try {
            groupId = Integer.parseInt(groupIdParam.trim());
        } catch (NumberFormatException e) {
            response.setStatus(400);
            response.getWriter().write("{\"success\": false, \"message\": \"Invalid group ID format: '" + escapeJson(groupIdParam) + "'\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psInsert = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getDBConnection();

            // 1. Check if user is already a member of this group
            String sqlCheck = """
                SELECT COUNT(*) as count 
                FROM Study_group_membership 
                WHERE group_id = ? AND user_id = ?
                """;

            psCheck = conn.prepareStatement(sqlCheck);
            psCheck.setInt(1, groupId);
            psCheck.setInt(2, userId);
            rs = psCheck.executeQuery();

            if (rs.next() && rs.getInt("count") > 0) {
                // User is already a member
                response.setStatus(400);
                response.getWriter().write("{\"success\": false, \"message\": \"You are already a member of this group\"}");
                return;
            }

            // Close the result set before next query
            rs.close();

            // 2. Verify that the group exists
            String sqlVerifyGroup = "SELECT group_id FROM Study_groups WHERE group_id = ?";
            psCheck = conn.prepareStatement(sqlVerifyGroup);
            psCheck.setInt(1, groupId);
            rs = psCheck.executeQuery();

            if (!rs.next()) {
                response.setStatus(404);
                response.getWriter().write("{\"success\": false, \"message\": \"Study group not found\"}");
                return;
            }

            // Close resources before final insert
            rs.close();
            psCheck.close();

            // 3. Add user to the group
            String sqlInsert = """
                INSERT INTO Study_group_membership (group_id, user_id) 
                VALUES (?, ?)
                """;

            psInsert = conn.prepareStatement(sqlInsert);
            psInsert.setInt(1, groupId);
            psInsert.setInt(2, userId);
            
            int affectedRows = psInsert.executeUpdate();

            if (affectedRows > 0) {
                response.setStatus(200);
                response.getWriter().write("{\"success\": true, \"message\": \"Successfully joined the study group\"}");
            } else {
                response.setStatus(500);
                response.getWriter().write("{\"success\": false, \"message\": \"Failed to join group\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            
            // Check if it's a duplicate key error (already a member via unique constraint)
            String errorMessage = e.getMessage().toLowerCase();
            if (errorMessage.contains("duplicate") || errorMessage.contains("unique")) {
                response.setStatus(400);
                response.getWriter().write("{\"success\": false, \"message\": \"You are already a member of this group\"}");
            } else {
                response.setStatus(500);
                response.getWriter().write("{\"success\": false, \"message\": \"Database error: " + escapeJson(e.getMessage()) + "\"}");
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (psCheck != null) psCheck.close();
                if (psInsert != null) psInsert.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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