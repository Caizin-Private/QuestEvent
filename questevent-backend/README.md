# QuestEvent Backend

QuestEvent Backend is a Spring Boot–based backend service that powers the QuestEvent platform.  
It provides APIs for program management, activity participation, reward handling, and ranking computation, with a strong focus on scalability, data consistency, and clean service boundaries.

---

## Overview

The backend is responsible for:
- Managing core domain entities
- Enforcing business rules
- Handling authentication and authorization
- Performing reward and ranking calculations
- Exposing REST APIs for frontend consumption

The service follows a layered architecture to ensure maintainability and extensibility.

---

## Key Features

- Program and activity management APIs
- User registration and participation handling
- Wallet-based reward accounting
- Program-level and global ranking computation
- Role-based access control
- JWT-based authentication
- Optimized database queries using JPA and JPQL

---

## Tech Stack

- **Language:** Java  
- **Framework:** Spring Boot  
- **Build Tool:** Maven  
- **Persistence:** JPA / Hibernate  
- **Database:** MySQL / PostgreSQL  
- **Security:** Spring Security with JWT  

---

## Project Structure

The backend follows a standard layered structure:

- **Controller Layer**  
  Handles HTTP requests, request validation, and response mapping.

- **Service Layer**  
  Contains core business logic and coordinates workflows.

- **Repository Layer**  
  Manages database interactions using JPA and custom queries.

- **Entity Layer**  
  Defines domain models such as User, Program, Activity, Wallet, and Submission.

- **DTOs**  
  Used for structured data transfer between layers and API responses.

---

## Getting Started

### Prerequisites

- Java 17 or above  
- Maven  
- MySQL or PostgreSQL  

### Running the Backend Locally

```bash
git clone https://github.com/your-organization/QuestEvent.git
cd QuestEvent/questevent-backend
mvn clean install
mvn spring-boot:run



