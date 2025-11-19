import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

@WebServlet(value = "/init-db", loadOnStartup = 1) // Runs automatically when Tomcat starts
public class DatabaseInitializer extends HttpServlet {

    @Override
    public void init() throws ServletException {
        createEventsTableIfNotExists();
    }

    private void createEventsTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS Group_events (
                event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                group_id INT NOT NULL,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                start_datetime DATETIME NOT NULL,
                end_datetime DATETIME,
                color VARCHAR(7) DEFAULT '#667eea',
                created_by INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (group_id) REFERENCES Study_groups(group_id) ON DELETE CASCADE,
                FOREIGN KEY (created_by) REFERENCES User(UserID),
                INDEX idx_group_events_group (group_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;

        try (Connection conn = DBConnection.getDBConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createTableSQL);
            System.out.println("Group_events table ready!");

        } catch (Exception e) {
            System.err.println("Could not create Group_events table:");
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write("Database initialized. Group_events table is ready!");
    }
}