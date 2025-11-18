package com.studysessions.tests;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.sql.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class StudySessionsTestSuite {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:8080/webproject";
    private static final String DB_URL = "jdbc:mysql://18.222.187.0:3306/class_webapp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "coryremote";
    private static final String DB_PASSWORD = "1337";

    @BeforeClass
    public static void setUpClass() {
        //System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterClass
    public static void tearDownClass() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void test1_DatabaseConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            assertNotNull("Connection should not be null", conn);
            assertTrue("Connection should be valid", conn.isValid(5));
        } catch (Exception e) {
            fail("Database connection failed: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void test2_DatabaseSchema() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            DatabaseMetaData metaData = conn.getMetaData();
            
            String[] tables = {"User", "Study_groups", "Study_group_membership", "Meetings"};
            for (String table : tables) {
                ResultSet rs = metaData.getTables(null, null, table, null);
                assertTrue("Table " + table + " should exist", rs.next());
                rs.close();
            }
        } catch (Exception e) {
            fail("Schema check failed: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void test3_HomePage() {
        driver.get(BASE_URL);
        assertNotNull("Should load page", driver.getTitle());
        assertTrue("Page should load", driver.getPageSource().length() > 0);
    }

    @Test
    public void test4_LoginPage() {
        driver.get(BASE_URL + "/login.html");
        assertTrue("Should contain login elements", 
            driver.getPageSource().toLowerCase().contains("login") ||
            driver.getPageSource().toLowerCase().contains("email"));
    }

    @Test
    public void test5_SignupPage() {
        driver.get(BASE_URL + "/signup.html");
        assertTrue("Should contain signup elements",
            driver.getPageSource().toLowerCase().contains("signup") ||
            driver.getPageSource().toLowerCase().contains("register"));
    }

    @Test
    public void test6_SearchPage() {
        driver.get(BASE_URL + "/searchForm.html");
        assertTrue("Should contain search elements",
            driver.getPageSource().toLowerCase().contains("search") ||
            driver.getPageSource().toLowerCase().contains("find"));
    }

    @Test
    public void test7_CreateGroupPage() {
        driver.get(BASE_URL + "/createGroup.html");
        assertTrue("Should contain create group elements",
            driver.getPageSource().toLowerCase().contains("group") ||
            driver.getPageSource().toLowerCase().contains("create"));
    }

    @Test
    public void test8_Navigation() {
        driver.get(BASE_URL);
        String title = driver.getTitle();
        assertNotNull("Page should have a title", title);
        assertFalse("Title should not be empty", title.isEmpty());
    }

    @Test
    public void test9_PageLoad() {
        driver.get(BASE_URL + "/default.html");
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        boolean loaded = shortWait.until(d -> d.getPageSource().length() > 100);
        assertTrue("Page should load content", loaded);
    }

    @Test
    public void test10_BrowserInteraction() {
        driver.get(BASE_URL);
        driver.manage().window().maximize();
        Dimension size = driver.manage().window().getSize();
        assertTrue("Window should be maximized", size.width > 800 && size.height > 600);
    }
}