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

@WebServlet("/api/groups/search")
public class SearchGroupsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String query = request.getParameter("query");
        
        if (query == null || query.trim().isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"success\": false, \"message\": \"Search query is required\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getDBConnection();

            // Search across multiple fields using LIKE
            String sql = """
                SELECT group_id, group_name, class_name, class_code, subject, descripton
                FROM Study_groups
                WHERE class_name LIKE ? 
                   OR class_code LIKE ? 
                   OR subject LIKE ? 
                   OR group_name LIKE ?
                ORDER BY class_code, class_name
                """;

            ps = conn.prepareStatement(sql);
            String searchPattern = "%" + query.trim() + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setString(4, searchPattern);

            rs = ps.executeQuery();

            // Build JSON array manually
            StringBuilder json = new StringBuilder();
            json.append("[");
            
            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                int groupId = rs.getInt("group_id");
                String groupName = escapeJson(rs.getString("group_name"));
                String className = escapeJson(rs.getString("class_name"));
                String classCode = escapeJson(rs.getString("class_code"));
                String subject = escapeJson(rs.getString("subject"));
                String description = escapeJson(rs.getString("descripton"));

                json.append("{");
                json.append("\"groupId\":").append(groupId).append(",");
                json.append("\"groupName\":\"").append(groupName).append("\",");
                json.append("\"className\":\"").append(className).append("\",");
                json.append("\"classCode\":\"").append(classCode).append("\",");
                json.append("\"subject\":\"").append(subject).append("\",");
                json.append("\"description\":\"").append(description != null ? description : "").append("\"");
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
                if (ps != null) ps.close();
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