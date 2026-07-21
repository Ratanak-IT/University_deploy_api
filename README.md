# 🎓 University Management System - Backend API

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.9-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Keycloak](https://img.shields.io/badge/Keycloak-IAM-4D4D4D?style=for-the-badge&logo=keycloak&logoColor=white)
![MinIO](https://img.shields.io/badge/MinIO-Object_Storage-C72C48?style=for-the-badge&logo=spring-boot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-Build_Tool-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Container-2496ED?style=for-the-badge&logo=docker&logoColor=white)

---

##  1. Project Overview

The **University Management System Backend** is an RESTful API engineered with **Spring Boot 3.5.9** and **Java 25**. Designed to serve as the core engine for higher education institutions, this platform automates and manages identity control, academic program structures, classroom enrollments, coursework submissions, online examinations, GPA calculations, digital certificates, and cloud media management.

---

##  2. Core Modules & Detailed Feature Breakdown

###  1. Identity & Access Management (`/identity`)
Centralized security powered by Keycloak IAM and Spring Security:
* **Federated Identity Management:** Full OAuth2 protocol support, JWT token handling, and Keycloak Admin Client integration.
* **Role-Based Access Control (RBAC):** Hierarchical user management supporting `Admin`, `Teacher`, and `Student` roles with dynamic permissions.
* **Authentication Security:** Proof Key for Code Exchange (PKCE) utility integration, secure session refresh, and explicit token revocation.

###  2. Student Academic Engine (`/student`)
Comprehensive management of student profiles and academic milestones:
* **Profile Management:** Detailed student records including personal data, graduation status, degree levels, and enrollment history.
* **GPA & Transcript Engine:** Automated calculation of semester and cumulative GPAs based on configurable grade scales.
* **Academic History:** Real-time generation of official student transcripts and course completion records.

###  3. Teacher & Department Management (`/teacher`, `/department`)
Faculty management and department organization:
* **Faculty Administration:** Faculty profile management, department assignment, and active status tracking.
* **Academic Workload Assignment:** Assigns teachers to specific subjects, courses, and active classrooms.

###  4. Classroom & Curriculum Management (`/classroom`, `/curriculum`, `/program`)
Organizational architecture for degree offerings and active classes:
* **Program & Curriculum Structuring:** Multi-year degree program planning, prerequisite management, and curriculum design.
* **Classroom Roster Control:** Class creation, student enrollment workflows, and teacher assignment.
* **Attendance Tracking:** Real-time logging and monitoring of student attendance statuses.

###  5. Coursework & Submissions (`/assignment`)
End-to-end assignment workflow management:
* **Assignment Creation:** Teacher publishing of assignments with attached guidelines and strict submission deadlines.
* **Student File Uploads:** Multi-file submission handling backed by object storage.
* **Grading & Feedback Engine:** Grading interface for teachers with maximum score validation and feedback threads.

###  6. Quiz & Examination Engine (`/quiz`, `/score`)
Online assessment and score recording:
* **Dynamic Quiz Creation:** Flexible question bank creation, time window parameters, and attempt limits.
* **Automated Quiz Attempt Grading:** Instant score evaluation and submission finalization.
* **Exam Score Management:** Entry and logging of midterm and final exam scores for transcript integration.

###  7. Digital Certificates & Material Streaming (`/certificate`, `/lesson`)
Academic documentation and lesson delivery:
* **Certificate Issuing:** Processing, approval, and digital issuance of official academic certificates.
* **File Streaming:** High-performance streaming, of course, material files and media directly to authenticated clients.

### ☁️ 8. Object Storage & Synchronization (`/minio`, `/sync`)
Cloud-native asset handling and data integrity:
* **MinIO S3 Integration:** Asynchronous file uploading, retrieval, and bucket management for assignments, lessons, and profile assets.
* **Keycloak Synchronization:** Automated background synchronization keeping Keycloak user attributes aligned with internal database entities.

---

##  3. Tech Stack & Infrastructure

* **Language:** Java 25
* **Core Framework:** Spring Boot 3.5.9
* **Identity Provider:** Keycloak (OAuth2 / OIDC / JWT)
* **Persistence Layer:** PostgreSQL, Spring Data JPA, Hibernate
* **Object Storage:** MinIO S3 API
* **Build System:** Gradle
* **Containerization & Deployment:** Docker, Dockerfile

---