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
import java.sql.Timestamp;

@WebServlet("/api/groups/events/get")
public class GetGroupEventsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Get groupId from parameter instead of path (easier for Tomcat)
        String groupIdParam = request.getParameter("groupId");
        if (groupIdParam == null || groupIdParam.trim().isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"success\": false, \"message\": \"Group ID is required\"}");
            return;
        }

        int groupId;
        try {
            groupId = Integer.parseInt(groupIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(400);
            response.getWriter().write("{\"success\": false, \"message\": \"Invalid group ID\"}");
            return;
        }

        // Get user from parameter
        String userJson = request.getParameter("user");
        if (userJson == null || userJson.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("{\"success\": false, \"message\": \"Not logged in\"}");
            return;
        }

        // Parse user ID
        int userId;
        try {
            String idStr = userJson.split("\"id\":")[1].split(",")[0].trim();
            userId = Integer.parseInt(idStr);
        } catch (Exception e) {
            response.setStatus(400);
            response.getWriter().write("{\"success\": false, \"message\": \"Invalid user data\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psEvents = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getDBConnection();

            // Verify user is a member of this group
            String sqlCheck = """
                SELECT COUNT(*) as count 
                FROM Study_group_membership 
                WHERE group_id = ? AND user_id = ?
                """;

            psCheck = conn.prepareStatement(sqlCheck);
            psCheck.setInt(1, groupId);
            psCheck.setInt(2, userId);
            rs = psCheck.executeQuery();

            if (rs.next() && rs.getInt("count") == 0) {
                response.setStatus(403);
                response.getWriter().write("{\"success\": false, \"message\": \"You are not a member of this group\"}");
                return;
            }

            rs.close();
            psCheck.close();

            // Get events for this group
            String sqlEvents = """
                SELECT event_id, title, description, start_datetime, end_datetime, color
                FROM Group_events
                WHERE group_id = ?
                ORDER BY start_datetime
                """;

            psEvents = conn.prepareStatement(sqlEvents);
            psEvents.setInt(1, groupId);
            rs = psEvents.executeQuery();

            // Build JSON array manually
            StringBuilder json = new StringBuilder();
            json.append("[");

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                int eventId = rs.getInt("event_id");
                String title = escapeJson(rs.getString("title"));
                String description = escapeJson(rs.getString("description"));
                Timestamp start = rs.getTimestamp("start_datetime");
                Timestamp end = rs.getTimestamp("end_datetime");
                String color = rs.getString("color");

                json.append("{");
                json.append("\"id\":").append(eventId).append(",");
                json.append("\"title\":\"").append(title).append("\",");
                json.append("\"description\":\"").append(description != null ? description : "").append("\",");
                json.append("\"start\":\"").append(start.toString().replace(" ", "T")).append("\",");
                
                if (end != null) {
                    json.append("\"end\":\"").append(end.toString().replace(" ", "T")).append("\",");
                }
                
                json.append("\"color\":\"").append(color != null ? color : "#667eea").append("\"");
                json.append("}");
            }

            json.append("]");

            response.setStatus(200);
            response.getWriter().write(json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"success\": false, \"message\": \"Database error: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try {
                if (rs != null) rs.close();
                if (psCheck != null) psCheck.close();
                if (psEvents != null) psEvents.close();
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