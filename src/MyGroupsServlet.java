import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/groups/my")
public class MyGroupsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Get user from localStorage (sent from frontend)
        String userJson = request.getParameter("user");
        if (userJson == null || userJson.isEmpty()) {
            response.setStatus(401);
            out.print("{\"success\":false,\"message\":\"Not logged in\"}");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(userJson.split("\"id\":")[1].split(",")[0]);
        } catch (Exception e) {
            response.setStatus(400);
            out.print("{\"success\":false,\"message\":\"Invalid user\"}");
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getDBConnection();

            String sql = """
            	    SELECT 
            	        sg.group_id,
            	        sg.group_name,
            	        sg.class_name,
            	        sg.class_code,
            	        sg.subject,
            	        sg.descripton,
            	        COUNT(sgm.user_id) as member_count
            	    FROM Study_groups sg
            	    JOIN Study_group_membership sgm ON sg.group_id = sgm.group_id
            	    WHERE sgm.user_id = ?
            	    GROUP BY 
            	        sg.group_id,
            	        sg.group_name,
            	        sg.class_name,
            	        sg.class_code,
            	        sg.subject,
            	        sg.descripton
            	    ORDER BY sg.class_code, sg.group_name
            	    """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"groups\":[");
            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");
                first = false;

                json.append("{")
                    .append("\"groupId\":").append(rs.getInt("group_id")).append(",")
                    .append("\"groupName\":\"").append(escape(rs.getString("group_name"))).append("\",")
                    .append("\"className\":\"").append(escape(rs.getString("class_name"))).append("\",")
                    .append("\"classCode\":\"").append(escape(rs.getString("class_code"))).append("\",")
                    .append("\"subject\":\"").append(escape(rs.getString("subject"))).append("\",")
                    .append("\"description\":\"").append(escape(rs.getString("descripton"))).append("\",")
                    .append("\"memberCount\":").append(rs.getInt("member_count"))
                    .append("}");
            }
            json.append("]}");

            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"success\":false,\"message\":\"Database error\"}");
        } finally {
            DBConnection.closeConnection();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}