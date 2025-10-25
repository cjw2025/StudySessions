import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading database properties from config.properties file
 */
public class UtilProp {
    
    private static Properties properties = new Properties();
    private static boolean loaded = false;
    
    /**
     * Load properties from config.properties file
     */
    public static void loadProperty() {
        if (loaded) {
            return; // Already loaded
        }
        
        try {
            // Try loading from classpath first
            InputStream input = UtilProp.class.getClassLoader().getResourceAsStream("config.properties");
            
            if (input == null) {
                // Try loading from file system
                input = new FileInputStream("config.properties");
            }
            
        
            // Load properties
            properties.load(input);
            loaded = true;
            
            System.out.println("Database properties loaded successfully from config.properties");
            input.close();
            
        } catch (IOException e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get property value by key
     * @param key Property key
     * @return Property value or null if not found
     */
    public static String getProp(String key) {
        if (!loaded) {
            loadProperty();
        }
        return properties.getProperty(key);
    }
    
    /**
     * Get property value with default
     * @param key Property key
     * @param defaultValue Default value if key not found
     * @return Property value or default value
     */
    public static String getProp(String key, String defaultValue) {
        if (!loaded) {
            loadProperty();
        }
        return properties.getProperty(key, defaultValue);
    }
}