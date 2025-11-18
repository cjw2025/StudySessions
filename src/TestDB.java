import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDB {

    public static void main(String[] args) {
        System.out.println("=== StudySession WebApp - Database Test ===\n");

        Connection conn = null;
        try {
            // This uses your existing DBConnection class
            conn = DBConnection.getDBConnection();
            System.out.println("Connected to database successfully!\n");

            Statement stmt = conn.createStatement();

            // -----------------------------
            // 1. Show All Users
            // -----------------------------
            System.out.println("USERS IN THE SYSTEM:");
            System.out.println("------------------------------------------------------------");
            String sqlUsers = "SELECT UserID, First_name, Last_name, Email, Phone FROM User ORDER BY Last_name, First_name";
            ResultSet rsUsers = stmt.executeQuery(sqlUsers);

            if (!rsUsers.isBeforeFirst()) {
                System.out.println("No users found. Table is empty.\n");
            } else {
                System.out.printf("%-5s %-15s %-15s %-30s %-15s%n", 
                    "ID", "First Name", "Last Name", "Email", "Phone");
                System.out.println("------------------------------------------------------------");
                while (rsUsers.next()) {
                    int id = rsUsers.getInt("UserID");
                    String first = rsUsers.getString("First_name");
                    String last = rsUsers.getString("Last_name");
                    String email = rsUsers.getString("Email");
                    String phone = rsUsers.getString("Phone");
                    if (phone == null) phone = "(none)";

                    System.out.printf("%-5d %-15s %-15s %-30s %-15s%n", 
                        id, first, last, email, phone);
                }
                System.out.println();
            }
            rsUsers.close();

            // -----------------------------
            // 2. Show All Study Groups
            // -----------------------------
            // Note: typo in your schema is "descripton" (missing i)
            System.out.println("STUDY GROUPS:");
            System.out.println("==================================================================================================");
            String sqlGroups = """
                SELECT group_id, group_name, class_name, class_code, subject, 
                       descripton, meeting_location, meeting_time, meeting_days 
                FROM Study_groups 
                ORDER BY subject, class_code
                """;
            ResultSet rsGroups = stmt.executeQuery(sqlGroups);

            if (!rsGroups.isBeforeFirst()) {
                System.out.println("No study groups found. Table is empty.\n");
            } else {
                System.out.printf("%-4s %-20s %-15s %-10s %-12s %-40s %-20s %-12s %-20s%n",
                    "ID", "Group Name", "Class Name", "Code", "Subject", "Description", "Location", "Time", "Days");
                System.out.println("==================================================================================================");
                while (rsGroups.next()) {
                    int id = rsGroups.getInt("group_id");
                    String gname = rsGroups.getString("group_name");
                    String cname = rsGroups.getString("class_name");
                    String code = rsGroups.getString("class_code");
                    String subj = rsGroups.getString("subject");
                    String desc = rsGroups.getString("descripton");
                    String loc = rsGroups.getString("meeting_location");
                    String time = rsGroups.getObject("meeting_time") != null ? 
                                  rsGroups.getTime("meeting_time").toString() : "(none)";
                    String days = rsGroups.getString("meeting_days");
                    if (desc == null) desc = "(no description)";
                    if (loc == null) loc = "(not set)";
                    if (days == null || days.isEmpty()) days = "(any day)";

                    System.out.printf("%-4d %-20s %-15s %-10s %-12s %-40s %-20s %-12s %-20s%n",
                        id, truncate(gname,20), truncate(cname,15), code, subj, 
                        truncate(desc,40), truncate(loc,20), time, days);
                }
                System.out.println();
            }
            rsGroups.close();

            System.out.println("All tests completed successfully!");

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection();
        }
    }

    // Helper to prevent super long text from messing up formatting
    private static String truncate(String s, int len) {
        if (s == null) return "(none)";
        return s.length() > len ? s.substring(0, len-3) + "..." : s;
    }
}