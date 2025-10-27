

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Application starting up...");
        
        try {
            // Use YOUR existing DBConnection class
            Connection connection = DBConnection.getDBConnection();
            
            // Store in context
            sce.getServletContext().setAttribute("dbConnection", connection);
            
            System.out.println("Database connection initialized");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database connection");
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application shutting down...");
        
        // Use YOUR existing DBConnection class
        DBConnection.closeConnection();
    }
}