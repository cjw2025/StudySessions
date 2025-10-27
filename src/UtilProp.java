import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading database properties from config.properties file
 * Updated to look in WebContent folder and other common locations
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
            InputStream input = null;
            
            // Try 1: Load from classpath (WEB-INF/classes/)
            input = UtilProp.class.getClassLoader().getResourceAsStream("config.properties");
            if (input != null) {
                System.out.println("[UtilProp] Loading from classpath");
            }
            
            // Try 2: Load from root of classpath
            if (input == null) {
                input = UtilProp.class.getResourceAsStream("/config.properties");
                if (input != null) {
                    System.out.println("[UtilProp] Loading from root classpath");
                }
            }
            
            // Try 3: Load from current working directory
            if (input == null) {
                try {
                    input = new FileInputStream("config.properties");
                    System.out.println("[UtilProp] Loading from current directory");
                } catch (Exception e) {
                    // Not found, try next location
                }
            }
            
            // Try 4: Load from WebContent folder (common in Eclipse projects)
            if (input == null) {
                try {
                    input = new FileInputStream("WebContent/config.properties");
                    System.out.println("[UtilProp] Loading from WebContent/");
                } catch (Exception e) {
                    // Not found, try next location
                }
            }
            
            // Try 5: Load from parent directory (if running from build folder)
            if (input == null) {
                try {
                    input = new FileInputStream("../WebContent/config.properties");
                    System.out.println("[UtilProp] Loading from ../WebContent/");
                } catch (Exception e) {
                    // Not found, try next location
                }
            }
            
            // Try 6: Absolute path in Eclipse workspace (modify path as needed)
            if (input == null) {
                try {
                    // This works when Tomcat is deployed by Eclipse
                    String[] possiblePaths = {
                        "wtpwebapps/webproject/config.properties",
                        "../wtpwebapps/webproject/config.properties",
                        "../../wtpwebapps/webproject/config.properties"
                    };
                    
                    for (String path : possiblePaths) {
                        try {
                            input = new FileInputStream(path);
                            if (input != null) {
                                System.out.println("[UtilProp] Loading from: " + path);
                                break;
                            }
                        } catch (Exception ignored) {
                            // Try next path
                        }
                    }
                } catch (Exception e) {
                    // Not found
                }
            }
            
            if (input == null) {
                System.err.println("Unable to find config.properties file!");
                System.err.println("Searched in:");
                System.err.println("  - Classpath (WEB-INF/classes/)");
                System.err.println("  - Current directory");
                System.err.println("  - WebContent/");
                System.err.println("  - ../WebContent/");
                System.err.println("  - Tomcat wtpwebapps/");
                System.err.println("");
                System.err.println("Current working directory: " + System.getProperty("user.dir"));
                return;
            }
            
            // Load properties
            properties.load(input);
            loaded = true;
            
            System.out.println("✓ Database properties loaded successfully");
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