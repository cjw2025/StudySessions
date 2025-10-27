import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String firstName = request.getParameter("first_name");
        String lastName = request.getParameter("last_name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");

        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        try {
            Connection conn = DBConnection.getDBConnection(); // assumes DBConnection.connection is set
            UserDAO userDAO = new UserDAO(conn);

            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhone(phone);

            User createdUser = userDAO.createUser(user, password);

            if (createdUser != null) {
                out.println("<html><body>");
                out.println("<h2>Registration Successful!</h2>");
                out.println("<p>Welcome, " + firstName + " " + lastName + "!</p>");
                out.println("</body></html>");
            } else {
                out.println("<html><body>");
                out.println("<h3>Error: Registration failed</h3>");
                out.println("</body></html>");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            out.println("<html><body>");
            out.println("<h3>Error: " + e.getMessage() + "</h3>");
            out.println("</body></html>");
        }
    }
}
