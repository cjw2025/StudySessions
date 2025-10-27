import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/search")
public class StudyGroupSearch extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public StudyGroupSearch() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String query = request.getParameter("query");
        if (query == null || query.trim().isEmpty()) {
            out.print("[]");
            return;
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            DBconnection2.getDBConnection();
            connection = DBconnection2.connection;

            String sql = "SELECT class_name, class_code, subject, group_name FROM Study_groups "
                       + "WHERE class_name LIKE ? OR class_code LIKE ? OR subject LIKE ?";

            preparedStatement = connection.prepareStatement(sql);
            String searchTerm = "%" + query.trim() + "%";
            preparedStatement.setString(1, searchTerm);
            preparedStatement.setString(2, searchTerm);
            preparedStatement.setString(3, searchTerm);

            ResultSet rs = preparedStatement.executeQuery();

            // Build JSON manually
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"className\":\"").append(rs.getString("class_name")).append("\",")
                    .append("\"classCode\":\"").append(rs.getString("class_code")).append("\",")
                    .append("\"subject\":\"").append(rs.getString("subject")).append("\",")
                    .append("\"groupName\":\"").append(rs.getString("group_name")).append("\"")
                    .append("}");
                first = false;
            }
            json.append("]");

            out.print(json.toString());
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error occurred.\"}");
        } finally {
            try {
                if (preparedStatement != null) preparedStatement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}

