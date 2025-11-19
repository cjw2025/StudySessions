import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.time.*;

@WebServlet("/api/groups/events/create")
public class CreateEventServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userJson = request.getParameter("user");
        if (userJson == null || userJson.isEmpty()) {
            response.setStatus(401);
            out.write("{\"success\":false,\"message\":\"Not logged in\"}");
            return;
        }

        // Extract userId from JSON
        int userId;
        try {
            String idStr = userJson.split("\"id\":")[1].split("[,}]")[0].trim();
            userId = Integer.parseInt(idStr);
        } catch (Exception e) {
            response.setStatus(400);
            out.write("{\"success\":false,\"message\":\"Invalid user data\"}");
            return;
        }

        String groupIdStr = request.getParameter("groupId");
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String startDateTimeStr = request.getParameter("startDateTime");
        String endDateTimeStr = request.getParameter("endDateTime");
        String color = request.getParameter("color");

        if (groupIdStr == null || title == null || startDateTimeStr == null ||
            groupIdStr.trim().isEmpty() || title.trim().isEmpty() || startDateTimeStr.trim().isEmpty()) {
            response.setStatus(400);
            out.write("{\"success\":false,\"message\":\"Missing required fields\"}");
            return;
        }

        int groupId = Integer.parseInt(groupIdStr.trim());
        Timestamp start = Timestamp.valueOf(startDateTimeStr.replace("T", " ") + ":00");
        Timestamp end = endDateTimeStr != null && !endDateTimeStr.trim().isEmpty()
                ? Timestamp.valueOf(endDateTimeStr.replace("T", " ") + ":00")
                : null;

        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psInsert = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getDBConnection();

            // Check if user is in the group
            String checkSql = "SELECT 1 FROM Study_group_membership WHERE group_id = ? AND user_id = ?";
            psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, groupId);
            psCheck.setInt(2, userId);
            rs = psCheck.executeQuery();

            if (!rs.next()) {
                response.setStatus(403);
                out.write("{\"success\":false,\"message\":\"You are not a member of this group\"}");
                return;
            }

            // INSERT with created_by and created_at
            String insertSql = """
                INSERT INTO Group_events 
                (group_id, title, description, start_datetime, end_datetime, color, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """;

            psInsert = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS);
            psInsert.setInt(1, groupId);
            psInsert.setString(2, title.trim());
            psInsert.setString(3, description != null && !description.trim().isEmpty() ? description.trim() : null);
            psInsert.setTimestamp(4, start);
            psInsert.setTimestamp(5, end);
            psInsert.setString(6, color != null && !color.trim().isEmpty() ? color.trim() : "#667eea");
            psInsert.setInt(7, userId); // THIS WAS MISSING!

            int rows = psInsert.executeUpdate();

            if (rows > 0) {
                ResultSet keys = psInsert.getGeneratedKeys();
                int eventId = keys.next() ? keys.getInt(1) : 0;
                out.write("{\"success\":true,\"message\":\"Event created!\",\"eventId\":" + eventId + "}");
            } else {
                out.write("{\"success\":false,\"message\":\"Failed to create event\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"Database error: " + escape(e.getMessage()) + "\"}");
        } finally {
            try { if (rs != null) rs.close(); if (psCheck != null) psCheck.close(); if (psInsert != null) psInsert.close(); }
            catch (Exception ignored) {}
            DBConnection.closeConnection();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}