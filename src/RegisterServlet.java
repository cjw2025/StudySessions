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
        
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        
        try {
            Connection conn = DBConnection.getDBConnection();
            
            if (conn == null || conn.isClosed()) {
                out.println("<html><body>");
                out.println("<h3>Error: Cannot connect to database</h3>");
                out.println("<p>Please check server logs</p>");
                out.println("<a href='signup.html'>Go back</a>");
                out.println("</body></html>");
                return;
            }
            
            UserDAO userDAO = new UserDAO(conn);
            
            // Check if user already exists
            User existingUser = userDAO.findByEmail(email);
            if (existingUser != null) {
                out.println("<html><body>");
                out.println("<h3>Error: Email already registered</h3>");
                out.println("<a href='signup.html'>Go back</a>");
                out.println("</body></html>");
                return;
            }
            
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhone(phone);
            
            User createdUser = userDAO.createUser(user);
            
            if (createdUser != null) {
                out.println("<html><body>");
                out.println("<h2>Registration Successful!</h2>");
                out.println("<p>Welcome, " + firstName + " " + lastName + "!</p>");
                out.println("<p>Your user ID is: " + createdUser.getUserId() + "</p>");
                out.println("<a href='index.html'>Go to Home</a>");
                out.println("</body></html>");
            } else {
                out.println("<html><body>");
                out.println("<h3>Error: Registration failed</h3>");
                out.println("<a href='signup.html'>Try again</a>");
                out.println("</body></html>");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("<html><body>");
            out.println("<h3>Database Error: " + e.getMessage() + "</h3>");
            out.println("<a href='signup.html'>Try again</a>");
            out.println("</body></html>");
        }
    }
}