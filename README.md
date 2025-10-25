/**
 * Data Access Object for User operations
 * Handles all database operations for users (CRUD operations)
 * 
 * Methods:
 * - createUser()        - Insert new user into database
 * - authenticateUser()  - Verify email/password and return user
 * - findByEmail()       - Find user by email address
 * - findById()          - Find user by ID
 * - updatePassword()    - Change user's password
 * - deleteUser()        - Remove user from database
 */

-- Create database
CREATE DATABASE IF NOT EXISTS class_webapp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant privileges to the remote application user (must be run as root user)
-- Log in with: mysql -u root -p
-- Then run:
-- GRANT ALL PRIVILEGES ON class_webapp_db.* TO 'coryjw'@'%';
-- FLUSH PRIVILEGES;

-- Use the database
USE class_webapp_db;

-- Create User table
CREATE TABLE User (
    UserID INT PRIMARY KEY AUTO_INCREMENT,
    First_name VARCHAR(100) NOT NULL,
    Last_name VARCHAR(100) NOT NULL,
    Email VARCHAR(255) NOT NULL UNIQUE,
    Phone VARCHAR(20)
);

ALTER TABLE User 
ADD COLUMN Password_hash VARCHAR(255) NOT NULL,

-- Create Study_groups table
CREATE TABLE Study_groups (
    group_id INT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    class_code VARCHAR(50) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    descripton TEXT,
    meeting_location VARCHAR(255),
    meeting_time TIME,
    meeting_days SET('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')
);

-- Create Study_group_membership table
CREATE TABLE Study_group_membership (
    membership_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id INT NOT NULL,
    user_id INT NOT NULL,
    FOREIGN KEY (group_id) REFERENCES Study_groups(group_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES User(UserID) ON DELETE CASCADE,
    UNIQUE KEY unique_membership (group_id, user_id)
);

-- Create Meetings table
CREATE TABLE Meetings (
    meeting_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    meeting_date DATE NOT NULL,
    FOREIGN KEY (group_id) REFERENCES Study_groups(group_id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_user_email ON User(Email);
CREATE INDEX idx_group_subject ON Study_groups(subject);
CREATE INDEX idx_membership_group ON Study_group_membership(group_id);
CREATE INDEX idx_membership_user ON Study_group_membership(user_id);
CREATE INDEX idx_meeting_group ON Meetings(group_id);
CREATE INDEX idx_meeting_date ON Meetings(meeting_date);
