# QuestEvent Application - Comprehensive Overview

## Application Purpose

**QuestEvent** is a comprehensive event and program management platform designed to facilitate competitive programming events, hackathons, and educational programs. The system enables organizations to create structured programs with multiple activities, manage participant registrations, track submissions, award rewards (gems), and maintain leaderboards. It serves as a gamified learning and competition platform where participants earn virtual currency (gems) by completing activities and can compete on both program-specific and global leaderboards.

---

## Core Functionality

### 1. **User Management & Authentication**
- **OAuth2 Integration**: Microsoft Azure AD authentication for secure user login
- **Role-Based Access Control (RBAC)**: Users have roles (Admin, Host, Participant, Judge) with different permission levels
- **User Profiles**: Comprehensive user profiles with department, gender, role, and contact information
- **JWT Token Authentication**: RESTful API authentication using JWT tokens for stateless API access
- **Session Management**: HTTP session-based authentication for web interface

### 2. **Program Management**
- **Program Creation**: Hosts can create programs with details including:
  - Title and description
  - Department categorization
  - Start and end dates
  - Registration fee
  - Program status (Draft, Active, Completed, Cancelled)
- **Program Lifecycle**: Full CRUD operations for program management
- **Program-Judge Association**: Each program is assigned a judge for activity evaluation
- **Program Hosting**: Users can host multiple programs, with each program linked to a host user

### 3. **Activity Management**
- **Activity Creation**: Activities are created within programs with:
  - Activity name and description
  - Rulebook (detailed rules and guidelines)
  - Activity duration
  - Reward gems (points awarded upon completion)
  - Compulsory/optional flag
- **Activity-Program Relationship**: Activities belong to programs, enabling structured multi-stage competitions
- **Activity Tracking**: System tracks activity creation timestamps and associations

### 4. **Registration System**
- **Program Registration**: 
  - Users can register for programs
  - Registration fee handling
  - Automatic program wallet creation upon registration
  - Unique constraint prevents duplicate registrations
  - Registration timestamp tracking
- **Activity Registration**:
  - Participants register for specific activities within programs
  - Completion status tracking (Pending, Completed, Not Completed)
  - Links participants to activities they're participating in

### 5. **Submission & Review System**
- **Activity Submissions**: 
  - Participants submit work via submission URLs
  - Submission timestamp tracking
  - One submission per activity registration (unique constraint)
- **Review Workflow**:
  - Submissions reviewed by assigned judges
  - Review status: Pending, Approved, Rejected
  - Judges can award gems based on submission quality
  - Review timestamp and reviewer tracking
- **Judging System**: Dedicated judge entities linked to programs for evaluation

### 6. **Wallet & Rewards System**
- **User Wallets**: 
  - Personal wallet for each user tracking total gems earned
  - Global gem balance across all programs
- **Program Wallets**:
  - Separate wallet per user per program
  - Tracks gems earned within specific programs
  - Wallet status tracking (Active, Settled, Expired)
  - Automatic wallet creation upon program registration
- **Gem Transactions**:
  - Gems awarded upon activity completion and submission approval
  - Transaction history tracking
  - Program wallet settlement (automatic and manual)
  - Scheduled settlement of expired program wallets

### 7. **Leaderboard System**
- **Global Leaderboard**: 
  - Ranks all users based on total gems and program participation
  - Cross-program competitive ranking
- **Program Leaderboard**: 
  - Program-specific rankings
  - Based on gems earned within that specific program
  - Encourages competition within program participants

### 8. **API Documentation**
- **Swagger/OpenAPI Integration**: 
  - Comprehensive API documentation using SpringDoc OpenAPI
  - Interactive API testing interface at `/swagger-ui`
  - API docs available at `/api-docs`
  - Detailed endpoint descriptions, request/response schemas, and examples

---

## Technology Stack

### **Backend Framework**
- **Spring Boot 3.3.5**: Modern Java application framework
- **Java 21**: Latest LTS version with modern language features
- **Maven**: Dependency management and build tool

### **Database & ORM**
- **PostgreSQL**: Relational database management system
- **JPA/Hibernate**: Object-Relational Mapping framework
- **Hibernate DDL Auto**: Automatic schema generation and updates
- **Database Migrations**: Schema managed through JPA entities

### **Security & Authentication**
- **Spring Security**: Comprehensive security framework
- **OAuth2 Client**: Microsoft Azure AD integration for OAuth2 authentication
- **OAuth2 Resource Server**: JWT token validation for API access
- **JWT (JSON Web Tokens)**: Stateless authentication using jjwt library (v0.12.3)
- **Method-Level Security**: `@PreAuthorize` annotations for fine-grained access control
- **Custom RBAC Service**: Role-based access control implementation

### **API & Documentation**
- **RESTful API**: REST architecture for all endpoints
- **SpringDoc OpenAPI 2.5.0**: API documentation and Swagger UI integration
- **Jackson**: JSON serialization/deserialization
- **CORS Configuration**: Cross-origin resource sharing support

### **Data Validation**
- **Bean Validation**: Jakarta Validation API for input validation
- **Spring Validation**: Framework-level validation support

### **Development Tools**
- **Lombok 1.18.36**: Reduces boilerplate code (getters, setters, constructors)
- **Spring Boot DevTools**: Hot reload and development utilities
- **JSpecify 1.0.0**: Null-safety annotations

### **Testing**
- **JUnit 5**: Unit and integration testing framework
- **Mockito**: Mocking framework for testing
- **Spring Boot Test**: Testing utilities and test slices
- **JaCoCo Maven Plugin**: Code coverage analysis

### **Build & CI/CD**
- **Maven Compiler Plugin**: Java compilation configuration
- **Spring Boot Maven Plugin**: Executable JAR packaging
- **GitHub Actions**: Continuous Integration pipeline
- **Maven Dependency Caching**: Optimized CI build times

---

## Architecture & Design Patterns

### **Layered Architecture**
- **Controller Layer**: RESTful API endpoints (`/api/*`)
- **Service Layer**: Business logic implementation
- **Repository Layer**: Data access using Spring Data JPA
- **Entity Layer**: JPA entities representing database tables
- **DTO Layer**: Data Transfer Objects for API requests/responses

### **Design Patterns Implemented**
- **Repository Pattern**: Spring Data JPA repositories for data access
- **Service Pattern**: Service classes encapsulating business logic
- **DTO Pattern**: Separate objects for API communication
- **Builder Pattern**: Lombok builders for object construction
- **Strategy Pattern**: Different service implementations (e.g., JudgeService, SubmissionService)

### **Database Design**
- **Relational Model**: Normalized database schema
- **Foreign Key Constraints**: Referential integrity enforcement
- **Unique Constraints**: Preventing duplicate registrations and submissions
- **Indexes**: Optimized query performance
- **Cascade Operations**: Automatic child entity management

### **Security Architecture**
- **Multi-Authentication Support**: OAuth2 for web, JWT for API
- **Method Security**: `@PreAuthorize` with custom RBAC expressions
- **CORS Configuration**: Configurable cross-origin policies
- **Session Management**: HTTP session for web authentication

---

## Key Features Implemented

### ✅ **Completed Features**

1. **User Authentication & Authorization**
   - Azure AD OAuth2 login
   - JWT token generation and validation
   - Role-based access control
   - Session management

2. **Program Management**
   - Create, read, update, delete programs
   - Program status management
   - Program-host association
   - Program-judge assignment

3. **Activity Management**
   - Activity CRUD operations
   - Activity-program relationships
   - Compulsory/optional activity flags
   - Reward gem configuration

4. **Registration System**
   - Program registration with fee handling
   - Activity registration
   - Duplicate prevention
   - Registration history tracking

5. **Submission System**
   - Activity submission via URLs
   - Submission status tracking
   - One submission per activity constraint

6. **Review & Judging**
   - Judge assignment to programs
   - Submission review workflow
   - Gem awarding based on reviews
   - Review status management

7. **Wallet System**
   - User wallet (global gems)
   - Program wallet (per-program gems)
   - Automatic wallet creation
   - Wallet balance tracking

8. **Transaction Management**
   - Gem transaction recording
   - Program wallet settlement
   - Scheduled settlement tasks
   - Manual settlement capability

9. **Leaderboard**
   - Global leaderboard
   - Program-specific leaderboard
   - Ranking algorithms

10. **API Documentation**
    - Swagger UI integration
    - OpenAPI 3.0 specification
    - Interactive API testing

11. **Testing Infrastructure**
    - Unit tests for services
    - Controller tests
    - Test coverage reporting (JaCoCo)
    - Mock-based testing

12. **CI/CD Pipeline**
    - GitHub Actions workflow
    - Automated testing on push/PR
    - Test coverage artifact upload
    - Maven build automation

---

## Database Schema Overview

### **Core Entities**
- **User**: User accounts with roles, departments, and profiles
- **Program**: Event programs with details and status
- **Activity**: Activities within programs
- **Judge**: Judges assigned to programs
- **ProgramRegistration**: User-program registrations
- **ActivityRegistration**: User-activity registrations
- **ActivitySubmission**: Submissions for activities
- **UserWallet**: Global user gem wallet
- **ProgramWallet**: Per-program user wallets
- **AllowedUser**: User access control (if implemented)

### **Relationships**
- User → Programs (One-to-Many: Host)
- Program → Activities (One-to-Many)
- Program → Judge (One-to-One)
- User → ProgramRegistrations (One-to-Many)
- User → ActivityRegistrations (One-to-Many)
- ActivityRegistration → ActivitySubmission (One-to-One)
- User → UserWallet (One-to-One)
- User → ProgramWallets (One-to-Many)
- Program → ProgramWallets (One-to-Many)

---

## API Endpoints Structure

### **Authentication**
- `GET /login` - Login page with OAuth2
- `GET /profile` - User profile page
- `POST /logout` - Logout endpoint
- `GET /api/auth/token` - JWT token generation

### **Programs**
- `POST /api/programs` - Create program
- `GET /api/programs` - List all programs
- `GET /api/programs/{id}` - Get program by ID
- `GET /api/programs/users/{userId}` - Get programs by user
- `PUT /api/programs/{id}` - Update program
- `DELETE /api/programs/{id}` - Delete program
- `POST /api/programs/{id}/settle` - Settle program wallets

### **Activities**
- `POST /api/activities` - Create activity
- `GET /api/activities` - List activities
- `GET /api/activities/{id}` - Get activity by ID
- `GET /api/activities/program/{programId}` - Get activities by program
- `PUT /api/activities/{id}` - Update activity
- `DELETE /api/activities/{id}` - Delete activity

### **Registrations**
- `POST /api/program-registrations` - Register for program
- `GET /api/program-registrations` - List registrations
- `GET /api/program-registrations/{id}` - Get registration by ID
- `DELETE /api/program-registrations/{id}` - Cancel registration
- `POST /api/activity-registrations` - Register for activity
- `GET /api/activity-registrations` - List activity registrations

### **Submissions**
- `POST /api/submissions` - Submit activity work
- `GET /api/submissions` - List submissions
- `GET /api/submissions/{id}` - Get submission by ID
- `PUT /api/submissions/{id}` - Update submission

### **Judging**
- `POST /api/judges` - Create judge
- `GET /api/judges` - List judges
- `PUT /api/judges/submissions/{submissionId}` - Review submission

### **Wallets**
- `GET /api/user-wallets/{userId}` - Get user wallet balance
- `GET /api/program-wallets/{programId}` - Get program wallet
- `GET /api/program-wallets/user/{userId}/program/{programId}` - Get user's program wallet

### **Leaderboard**
- `GET /api/leaderboard/global` - Global leaderboard
- `GET /api/leaderboard/program/{programId}` - Program leaderboard

### **Users**
- `GET /api/users` - List users
- `GET /api/users/{id}` - Get user by ID

---

## Development Workflow

### **Local Development**
1. Clone repository
2. Configure PostgreSQL database connection in `application.yml`
3. Set up Azure AD OAuth2 credentials
4. Run `mvn clean install` to build
5. Run Spring Boot application
6. Access Swagger UI at `http://localhost:8080/swagger-ui`

### **Testing**
- Run tests: `mvn test`
- View coverage: `target/site/jacoco/index.html`
- Test coverage reports generated automatically

### **CI/CD**
- GitHub Actions triggers on push to `master` branch
- Automated build and test execution
- Test coverage artifacts uploaded
- Ready for deployment integration

---

## Configuration Files

### **application.yml**
- Database connection configuration
- JPA/Hibernate settings
- OAuth2 Azure AD configuration
- JWT secret and expiration settings
- Swagger/OpenAPI configuration
- Server session configuration

### **pom.xml**
- Maven dependencies
- Java version configuration
- Build plugins (compiler, Spring Boot, JaCoCo)
- Dependency versions

### **GitHub Actions**
- CI workflow for automated testing
- Maven caching for faster builds
- Test coverage artifact upload

---

## Security Features

1. **Authentication Methods**
   - OAuth2 (Azure AD) for web interface
   - JWT tokens for API access
   - Session-based authentication

2. **Authorization**
   - Role-based access control (RBAC)
   - Method-level security annotations
   - Custom permission checks

3. **Data Protection**
   - Password encryption (handled by OAuth2 provider)
   - JWT secret key management
   - Secure session handling

4. **API Security**
   - CORS configuration
   - CSRF protection (disabled for API, enabled for web)
   - Authentication required for protected endpoints

---

## Future Enhancement Opportunities

While production deployment is excluded from this overview, the application is architected to support:
- Horizontal scaling
- Database connection pooling
- Caching mechanisms
- Message queue integration
- Microservices architecture migration
- Advanced analytics and reporting
- Real-time notifications
- File upload capabilities
- Email notifications
- Advanced search and filtering

---

## Summary

QuestEvent is a feature-rich, production-ready event management platform built with modern Java technologies. It provides comprehensive functionality for program and activity management, participant tracking, submission review, reward distribution, and competitive leaderboards. The application follows industry best practices with layered architecture, comprehensive security, thorough testing, and excellent API documentation. The codebase is well-structured, maintainable, and ready for team collaboration and future enhancements.
