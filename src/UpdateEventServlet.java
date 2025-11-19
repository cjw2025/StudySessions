import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

@WebServlet("/api/groups/events/update")
public class UpdateEventServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // === 1. Get and parse user (same pattern as your other servlets) ===
        String userJson = request.getParameter("user");
        if (userJson == null || userJson.isEmpty()) {
            response.setStatus(401);
            out.write("{\"success\":false,\"message\":\"Not logged in\"}");
            return;
        }

        int userId;
        try {
            String idStr = userJson.split("\"id\":")[1].split("[,}]")[0].trim();
            userId = Integer.parseInt(idStr);
        } catch (Exception e) {
            response.setStatus(400);
            out.write("{\"success\":false,\"message\":\"Invalid user data\"}");
            return;
        }

        // === 2. Get parameters ===
        String eventIdStr = request.getParameter("eventId");
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String startDateTimeStr = request.getParameter("startDateTime");
        String endDateTimeStr = request.getParameter("endDateTime");
        String color = request.getParameter("color");

        if (eventIdStr == null || title == null || startDateTimeStr == null ||
            eventIdStr.trim().isEmpty() || title.trim().isEmpty() || startDateTimeStr.trim().isEmpty()) {
            response.setStatus(400);
            out.write("{\"success\":false,\"message\":\"Missing required fields\"}");
            return;
        }

        int eventId;
        try {
            eventId = Integer.parseInt(eventIdStr.trim());
        } catch (NumberFormatException e) {
            response.setStatus(400);
            out.write("{\"success\":false,\"message\":\"Invalid event ID\"}");
            return;
        }

        // Convert datetime-local strings (YYYY-MM-DDTHH:MM) → Timestamp
        Timestamp start = Timestamp.valueOf(startDateTimeStr.replace("T", " ") + ":00");
        Timestamp end = (endDateTimeStr != null && !endDateTimeStr.trim().isEmpty())
                ? Timestamp.valueOf(endDateTimeStr.replace("T", " ") + ":00")
                : null;

        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getDBConnection();

            // === 3. Security: Make sure the user is allowed to edit this event ===
            // Must be a member of the group that owns the event
            String checkSql = """
                SELECT 1 
                FROM Group_events e
                JOIN Study_group_membership m ON e.group_id = m.group_id
                WHERE e.event_id = ? AND m.user_id = ?
                """;

            psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, eventId);
            psCheck.setInt(2, userId);
            rs = psCheck.executeQuery();

            if (!rs.next()) {
                response.setStatus(403);
                out.write("{\"success\":false,\"message\":\"You do not have permission to edit this event\"}");
                return;
            }

            // === 4. Perform the update ===
            String updateSql = """
                UPDATE Group_events
                SET title = ?, description = ?, start_datetime = ?, end_datetime = ?, color = ?
                WHERE event_id = ?
                """;

            psUpdate = conn.prepareStatement(updateSql);
            psUpdate.setString(1, title.trim());
            psUpdate.setString(2, description != null && !description.trim().isEmpty() ? description.trim() : null);
            psUpdate.setTimestamp(3, start);
            psUpdate.setTimestamp(4, end);
            psUpdate.setString(5, color != null && !color.trim().isEmpty() ? color.trim() : "#667eea");
            psUpdate.setInt(6, eventId);

            int rows = psUpdate.executeUpdate();

            if (rows > 0) {
                out.write("{\"success\":true,\"message\":\"Event updated successfully\"}");
            } else {
                out.write("{\"success\":false,\"message\":\"Event not found or no changes made\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.write("{\"success\":false,\"message\":\"Database error\"}");
        } finally {
            try { if (rs != null) rs.close(); }
            catch (Exception ignored) {}
            try { if (psCheck != null) psCheck.close(); }
            catch (Exception ignored) {}
            try { if (psUpdate != null) psUpdate.close(); }
            catch (Exception ignored) {}
            DBConnection.closeConnection();
        }
    }
}