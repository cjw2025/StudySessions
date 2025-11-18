import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/api/groups/create")
public class CreateGroupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Get logged-in user from session or localStorage (we use localStorage on frontend)
        String userJson = request.getParameter("user");
        if (userJson == null || userJson.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("{\"success\": false, \"message\": \"Not logged in\"}");
            return;
        }

        // Parse user ID from JSON (sent from frontend)
        int userId;
        try {
            // Simple parse: {"id":1,"firstName":"Cory",...} → extract id
            String idStr = userJson.split("\"id\":")[1].split(",")[0];
            userId = Integer.parseInt(idStr);
        } catch (Exception e) {
            response.setStatus(400);
            response.getWriter().write("{\"success\": false, \"message\": \"Invalid user data\"}");
            return;
        }

        // Get form data
        String groupName = request.getParameter("groupName");
        String className = request.getParameter("className");
        String classCode = request.getParameter("classCode");
        String subject = request.getParameter("subject");
        String description = request.getParameter("description");

        if (groupName == null || groupName.trim().isEmpty() ||
            className == null || className.trim().isEmpty() ||
            classCode == null || classCode.trim().isEmpty() ||
            subject == null || subject.trim().isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"success\": false, \"message\": \"All required fields must be filled\"}");
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getDBConnection();

            // 1. Insert into Study_groups
            String sqlGroup = """
                INSERT INTO Study_groups 
                (group_name, class_name, class_code, subject, descripton) 
                VALUES (?, ?, ?, ?, ?)
                """;

            PreparedStatement psGroup = conn.prepareStatement(sqlGroup, PreparedStatement.RETURN_GENERATED_KEYS);
            psGroup.setString(1, groupName.trim());
            psGroup.setString(2, className.trim());
            psGroup.setString(3, classCode.trim().toUpperCase());
            psGroup.setString(4, subject);
            psGroup.setString(5, description != null ? description.trim() : null);

            int affectedRows = psGroup.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating group failed, no rows affected.");
            }

            // Get the generated group_id
            int groupId;
            try (var generatedKeys = psGroup.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    groupId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating group failed, no ID obtained.");
                }
            }

            // 2. Automatically add creator as member
            String sqlMember = """
                INSERT INTO Study_group_membership (group_id, user_id) 
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE group_id = group_id
                """;

            PreparedStatement psMember = conn.prepareStatement(sqlMember);
            psMember.setInt(1, groupId);
            psMember.setInt(2, userId);
            psMember.executeUpdate();

            // Success!
            response.setStatus(200);
            response.getWriter().write("{\"success\": true, \"groupId\": " + groupId + "}");

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"success\": false, \"message\": \"Database error: " + e.getMessage() + "\"}");
        } finally {
            DBConnection.closeConnection();
        }
    }
}