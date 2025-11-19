import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

@WebServlet("/check-events-table")
public class CheckEventsTable extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<pre style='font-family: monospace; background: #f4f4f4; padding: 20px; border-radius: 8px;'>");
        out.println("CHECKING Group_events TABLE...\n");

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getDBConnection();
            stmt = conn.createStatement();

            // Try to query the table
            rs = stmt.executeQuery("SELECT * FROM Group_events LIMIT 0");
            ResultSetMetaData meta = rs.getMetaData();

            out.println("TABLE EXISTS!\n");
            out.println("Columns (" + meta.getColumnCount() + " total):\n");

            for (int i = 1; i <= meta.getColumnCount(); i++) {
                out.printf("  %2d. %-20s %-12s %s%n",
                    i,
                    meta.getColumnName(i),
                    meta.getColumnTypeName(i),
                    (meta.isNullable(i) == ResultSetMetaData.columnNoNulls ? "(NOT NULL)" : "")
                );
            }

            out.println("\nSUCCESS: Your calendar table is ready!");

        } catch (Exception e) {
            out.println("TABLE DOES NOT EXIST OR ERROR!");
            out.println("Error: " + e.getMessage());
            out.println("\nRun this once: http://localhost:8080/webproject/init-db");
            out.println("Then refresh this page.");
        } finally {
            try { if (rs != null) rs.close(); if (stmt != null) stmt.close(); } catch (Exception ignored) {}
            DBConnection.closeConnection();
        }

        out.println("</pre>");
        out.println("<p><a href='/webproject/myGroups.html'>Back to My Groups</a></p>");
    }
}