import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.regex.*;

@WebServlet("/api/groups/events/")
public class GetEventsServlet extends HttpServlet {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userJson = request.getParameter("user");
        if (userJson == null || userJson.isEmpty()) {
            response.setStatus(401);
            out.write("{\"success\":false,\"message\":\"Not logged in\"}");
            return;
        }

        int userId;
        try {
            Matcher m = ID_PATTERN.matcher(userJson);
            if (!m.find()) throw new Exception();
            userId = Integer.parseInt(m.group(1));
        } catch (Exception e) {
            response.setStatus(400);
            out.write("{\"success\":false,\"message\":\"Invalid user data\"}");
            return;
        }

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getDBConnection();

            String sql = """
                SELECT e.event_id, e.title, e.description, e.start_datetime, e.end_datetime, e.color,
                       sg.group_id, sg.group_name
                FROM Group_events e
                JOIN Study_groups sg ON e.group_id = sg.group_id
                JOIN Study_group_membership m ON sg.group_id = m.group_id
                WHERE m.user_id = ?
                ORDER BY e.start_datetime
                """;

            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                if (!first) json.append(",");
                first = false;

                String title = rs.getString("title");
                String groupName = rs.getString("group_name");
                String description = rs.getString("description");
                Timestamp start = rs.getTimestamp("start_datetime");
                Timestamp end = rs.getTimestamp("end_datetime");
                String color = rs.getString("color") != null ? rs.getString("color") : "#667eea";

                json.append("{")
                    .append("\"id\":").append(rs.getInt("event_id")).append(",")
                    .append("\"title\":\"").append(escape(title + " (" + groupName + ")")).append("\",")
                    .append("\"description\":\"").append(escape(description != null ? description : "")).append("\",")
                    .append("\"start\":\"").append(start.toString()).append("\",")
                    .append("\"end\":\"").append(end != null ? end.toString() : "").append("\",")
                    .append("\"color\":\"").append(color).append("\"")
                    .append("}");
            }
            json.append("]");

            out.write(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.write("{\"success\":false,\"message\":\"Database error\"}");
        } finally {
            try { if (rs != null) rs.close(); if (ps != null) ps.close(); } catch (Exception ignored) {}
            DBConnection.closeConnection();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}