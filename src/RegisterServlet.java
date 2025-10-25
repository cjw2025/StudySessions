import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String firstName = request.getParameter("first_name");
        String lastName = request.getParameter("last_name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");

        try {
			DBConnection.getDBConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Connection conn = DBConnection.connection;

        try {
            Connection connection = null;
            DBConnection.getDBConnection();
            connection = DBConnection.connection;

            String sql = "INSERT INTO myTable (id, MYUSER, EMAIL, PHONE) VALUES (default, ?, ?, ?)";
            PreparedStatement preparedStmt = connection.prepareStatement(sql);
            String fullName = firstName + " " + lastName;
            preparedStmt.setString(1, fullName);
            preparedStmt.setString(2, email);
            preparedStmt.setString(3, phone);

            preparedStmt.execute();

            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h2>Registration Successful!</h2>");
            out.println("<p>Welcome, " + firstName + " " + lastName + "</p>");
            out.println("</body></html>");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Error: " + e.getMessage());
        }
    }
}