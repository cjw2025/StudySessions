# Project to allow users to find study groups based on subject



[StudySessionsMySQL.txt](https://github.com/user-attachments/files/23061867/StudySessionsMySQL.txt)
The MySQL I used to create the database for StudySession WebApp

-- Create database
CREATE DATABASE IF NOT EXISTS class_webapp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- Create User table
CREATE TABLE User (
    UserID INT PRIMARY KEY AUTO_INCREMENT,
    First_name VARCHAR(100) NOT NULL,
    Last_name VARCHAR(100) NOT NULL,
    Email VARCHAR(255) NOT NULL UNIQUE,
    Phone VARCHAR(20)
);

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

CREATE TABLE Meetings (
    meeting_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id INT NOT NULL,
    meeting_date DATE NOT NULL,
    FOREIGN KEY (group_id) REFERENCES Study_groups(group_id) ON DELETE CASCADE
);


INDEXES

CREATE INDEX idx_user_email ON User(Email);
CREATE INDEX idx_group_subject ON Study_groups(subject);
CREATE INDEX idx_membership_group ON Study_group_membership(group_id);
CREATE INDEX idx_membership_user ON Study_group_membership(user_id);
CREATE INDEX idx_meeting_group ON Meetings(group_id);
CREATE INDEX idx_meeting_date ON Meetings(meeting_date);
