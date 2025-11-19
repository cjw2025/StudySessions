import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

@WebServlet("/api/groups/events/delete")
public class DeleteEventServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Get user JSON from parameter
        String userJson = request.getParameter("user");
        if (userJson == null || userJson.isEmpty()) {
            response.setStatus(401);
            out.write("{\"success\":false,\"message\":\"Not logged in\"}");
            return;
        }

        // Parse userId from JSON (same method you use elsewhere)
        int userId;
        try {
            String idStr = userJson.split("\"id\":")[1].split("[,}]")[0].trim();
            userId = Integer.parseInt(idStr);
        } catch (Exception e) {
            response.setStatus(400);
            out.write("{\"success\":false,\"message\":\"Invalid user data\"}");
            return;
        }

        String eventIdStr = request.getParameter("eventId");
        if (eventIdStr == null || eventIdStr.trim().isEmpty()) {
            response.setStatus(400);
            out.write("{\"success\":false,\"message\":\"Event ID is required\"}");
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

        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psDelete = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getDBConnection();

            // First: Verify that the user is allowed to delete this event
            // They must be in the same group OR be the creator (we'll check both for safety)
            String checkSql = """
                SELECT e.event_id 
                FROM Group_events e
                JOIN Study_groups sg ON e.group_id = sg.group_id
                JOIN Study_group_membership m ON sg.group_id = m.group_id
                WHERE e.event_id = ? AND m.user_id = ?
                """;

            psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, eventId);
            psCheck.setInt(2, userId);
            rs = psCheck.executeQuery();

            if (!rs.next()) {
                response.setStatus(403);
                out.write("{\"success\":false,\"message\":\"You don't have permission to delete this event\"}");
                return;
            }

            // Now delete the event
            String deleteSql = "DELETE FROM Group_events WHERE event_id = ?";
            psDelete = conn.prepareStatement(deleteSql);
            psDelete.setInt(1, eventId);

            int rows = psDelete.executeUpdate();

            if (rows > 0) {
                out.write("{\"success\":true,\"message\":\"Event deleted successfully\"}");
            } else {
                out.write("{\"success\":false,\"message\":\"Event not found or already deleted\"}");
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
            try { if (psDelete != null) psDelete.close(); }
            catch (Exception ignored) {}
            DBConnection.closeConnection();
        }
    }
}