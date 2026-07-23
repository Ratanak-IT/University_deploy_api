# University Management System (UMS)

A modern, centralized, and user-friendly web platform that enhances teaching, learning, and university administration. UMS enables students, teachers, and administrators to efficiently manage academic activities, communicate, monitor progress, and access educational resources from a single platform.

## Table of Contents

- [About the Project](#about-the-project)
- [Goals](#goals)
- [Tech Stack](#tech-stack)
- [Features](#features)
    - [Authentication & Security](#authentication--security)
    - [Administrator Module](#administrator-module)
    - [Teacher Module](#teacher-module)
    - [Student Module](#student-module)
    - [Backend Architecture](#backend-architecture)
    - [Frontend](#frontend)
- [How the System Operates](#how-the-system-operates)
    - [1. User Authentication](#1-user-authentication)
    - [2. Authorization](#2-authorization)
    - [3. Administrator Operation](#3-administrator-operation)
    - [4. Student Operation](#4-student-operation)
    - [5. Teacher Operation](#5-teacher-operation)
    - [6. Academic Management](#6-academic-management)
    - [7. Assignment Operation](#7-assignment-operation)
    - [8. Quiz Operation](#8-quiz-operation)
    - [9. Attendance Operation](#9-attendance-operation)
    - [10. Certificate Operation](#10-certificate-operation)
    - [11. Logout Operation](#11-logout-operation)

## About the Project

The University Management System (UMS) is built to centralize academic and administrative management for a university, providing secure, role-based access for **Administrators**, **Teachers**, and **Students**.

## Goals

- Centralize academic and administrative management.
- Provide secure authentication and role-based authorization.
- Improve communication between students, teachers, and administrators.
- Automate academic processes such as enrollment, grading, attendance, assignments, quizzes, transcripts, and certificate requests.
- Build a scalable and maintainable architecture using modern technologies.

## Tech Stack

**Backend:** Spring Boot &middot; Spring Security &middot; JWT Authentication &middot; Keycloak &middot; PostgreSQL &middot; JPA/Hibernate &middot; RESTful APIs &middot; Minio;

## Features

### Authentication & Security

- User Login
- JWT Authentication
- Refresh Token
- Logout
- Current User Profile
- Change Password
- Role-Based Access Control (RBAC)

### Administrator Module

- User Management
- Student Management (CRUD)
- Teacher Management (CRUD)
- Department Management
- Program Management
- Curriculum Management
- Classroom Management
- Subject Management
- Assignment Management
- Attendance Monitoring
- Certificate Management
- Transcript Management
- Notification Management
- User Login History

### Teacher Module

- Teacher Dashboard
- Assigned Classrooms
- Assigned Subjects
- Lesson Management
- Assignment Management
- Quiz Management
- Attendance Management
- Student Grading
- Department Assignment

### Student Module

- Academic Transcript
- GPA
- Grades
- Student Dashboard
- Profile Management
- Video Lessons
- Assignments
- Quizzes
- Bookmarks
- Notifications
- Attendance Tracking
- Grades & Results
- Certificates

### Backend Architecture

- Spring Boot REST APIs
- Spring Security
- JWT Authentication
- PostgreSQL
- JPA/Hibernate
- Layered Architecture
- DTO Mapping
- Validation
- Exception Handling
- Pagination Support

### Frontend

- Next.js (App Router)
- Redux Query
- Responsive Dashboard
- Modular UI Components
- Data Tables
- Dashboard Analytics
- Certificate Generator
- Transcript Viewer

## How the System Operates

The University Management System (UMS) operates through a secure role-based workflow that supports three primary user roles: **Administrator**, **Teacher**, and **Student**. Every user must authenticate before accessing the system. After successful authentication, the system authorizes the user according to their assigned role and grants access only to the permitted modules. The backend is built with Spring Boot REST APIs, Spring Security, JWT Authentication, and PostgreSQL, following a layered architecture to ensure scalability, maintainability, and security.

### 1. User Authentication

**Purpose:** Authenticate users before allowing access to the system.

**Workflow:**

1. User enters email and password.
2. Frontend sends the login request to the Authentication API.
3. Spring Security validates the request.
4. The system verifies the user's credentials.
5. User roles and permissions are loaded.
6. JWT Access Token and Refresh Token are generated.
7. Login history is recorded.
8. Tokens are returned to the frontend.
9. Frontend stores the Access Token.
10. User is redirected to the appropriate dashboard.

### 2. Authorization

After login, every request must include a valid JWT Access Token. The backend performs the following steps:

1. Read Authorization Header.
2. Validate JWT Token.
3. Verify token expiration.
4. Identify current user.
5. Load assigned role.
6. Check endpoint permission.
7. Allow or reject the request.
8. Return response.

If authorization fails, the system returns:

| Status Code | Meaning |
|---|---|
| `401` | Unauthorized |
| `403` | Forbidden |

### 3. Administrator Operation

The Administrator manages the entire university system, including:

- Managing all users
- Creating student accounts, teacher accounts
- Managing departments, programs, subjects, assignments, quizzes
- Creating curriculum, classrooms
- Assigning teachers, students
- Monitoring attendance
- Reviewing certificates, viewing login history

### 4. Student Operation

Students use the system to manage their academic activities:

- Viewing personal profile
- Updating profile information
- Viewing enrolled subjects, attendance records, academic transcript, GPA, grades, assignments
- Accessing learning materials
- Uploading assignment submissions
- Taking quizzes and viewing quiz results
- Requesting certificates and downloading approved certificates

### 5. Teacher Operation

Teachers manage academic activities for assigned classrooms:

- Managing assigned classrooms and subjects
- Creating lessons
- Uploading learning materials
- Creating assignments
- Reviewing assignment submissions
- Grading assignments
- Creating quizzes
- Managing attendance
- Publishing student grades

### 6. Academic Management

The Academic Management module enables administrators to manage the university's academic structure and organizational data. Administrators are responsible for creating and maintaining departments, academic programs, subjects, curricula, and classrooms. After the academic structure is established, administrators assign teachers to their respective departments, subjects, and classrooms, and enroll students into the appropriate academic programs and classes.

These operations ensure that all academic information is properly organized and connected, providing a complete academic structure that supports teaching, learning, enrollment, scheduling, grading, attendance, assignments, quizzes, and other educational activities throughout the University Management System.

### 7. Assignment Operation

The Assignment Management module enables teachers to create and distribute assignments to students enrolled in their classrooms. Teachers can define assignment details, attach learning materials, and specify submission deadlines.

Once an assignment is published, it becomes available for students to view through their dashboard. Students complete the assignment and upload their submission files before the deadline. The system stores all submissions securely and records the submission time. Teachers can review submitted work, provide grades and feedback, and publish the results. Students can then access their assignment scores and feedback through the system.

### 8. Quiz Operation

The Quiz Management module allows teachers to create quizzes and assign them to specific classrooms or subjects. Teachers configure quiz information, questions, marks, and time limits before publishing the quiz.

Students can view available quizzes, start an attempt, answer the questions, and submit their responses within the allowed time. After submission, the system automatically evaluates objective questions and calculates the student's score. The quiz attempt, score, and submission history are stored in the database. Students can later view their quiz results and performance summary.

### 9. Attendance Operation

The Attendance Management module allows teachers to record student attendance for each classroom session. Teachers select the classroom and lesson before marking each student's attendance status, such as **Present**, **Absent**, or **Late**.

After submission, the attendance records are stored in the database and become part of the student's academic record. Students can access their attendance history through their dashboard to monitor their attendance percentage and academic participation.

### 10. Certificate Operation

The Certificate Management module enables students to request official academic documents such as transcripts or enrollment certificates. Students submit a certificate request through the system, and the request is recorded with a pending status.

Administrators review each request and decide whether to approve or reject it. Once approved, the system generates the certificate document, stores it securely, and makes it available for download. Students can then access and download the approved certificate directly from their account.

### 11. Logout Operation

The Logout module securely terminates the user's authenticated session. When the user selects the logout option, the frontend sends a logout request to the backend. The backend invalidates the user's refresh token, clears the active session, and prevents further use of the existing authentication tokens.

The frontend removes the stored JWT Access Token and Refresh Token from local storage or cookies. Finally, the user is redirected to the login page, ensuring that protected resources can no longer be accessed without re-authentication.
