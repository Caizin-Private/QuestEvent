# QuestEvent

QuestEvent is a modern, backend-driven platform for managing gamified programs and activities with structured participation, reward distribution, and ranking mechanisms.

QuestEvent is designed to be **scalable, role-aware, and extensible**, making it suitable for real-world event, learning, or engagement platforms.

---

## Why QuestEvent?

QuestEvent provides a structured approach to running programs where participation, validation, rewards, and rankings are managed in a controlled and transparent manner.

QuestEvent can be used to build systems such as:
- Gamified learning platforms
- Community engagement programs
- Skill challenges and competitions
- Corporate or academic event platforms

---

## What QuestEvent Provides

- Program and activity management
- Structured user participation and registrations
- Wallet-based reward accounting
- Program-level and platform-level rankings
- Role-based access control
- Backend-driven evaluation and scoring
- Clean and maintainable service boundaries

---

## Core Capabilities

### Program Management
- Create and manage multiple programs
- Configure activities within programs
- Support compulsory and optional activities

### Participation Handling
- User registration at program and activity levels
- Controlled participation lifecycle
- Enforced participation rules

### Reward System
- Wallet-based accounting model
- Program wallets and user wallets
- Controlled reward allocation and settlement

### Rankings
- Program-specific rankings
- Global platform rankings
- Rankings computed dynamically from validated data

### Security & Access Control
- JWT-based authentication
- Role-based authorization
- Clear separation of user responsibilities

---

## Tech Stack

- **Language:** Java  
- **Framework:** Spring Boot  
- **Persistence:** JPA / Hibernate  
- **Database:** Relational (MySQL / PostgreSQL)  
- **Authentication:** JWT-based security  

---

## Project Structure

QuestEvent follows a layered backend design:

- **Controller Layer** – Request handling and validation  
- **Service Layer** – Business logic and orchestration  
- **Repository Layer** – Database access and queries  
- **Entity Layer** – Core domain models  

This structure ensures clarity, testability, and long-term maintainability.

---

## Installation & Setup

### Prerequisites
- Java 17+
- Maven
- MySQL or PostgreSQL

### Run Locally

```bash
git clone https://github.com/your-organization/QuestEvent.git
cd QuestEvent/questevent-backend
mvn clean install
mvn spring-boot:run

---

## Testing

The QuestEvent backend uses **JUnit 5** and **Mockito** for unit and integration testing.
Test execution and coverage metrics are integrated with **JaCoCo** and **SonarQube**.

### What Is Tested

#### Service Layer
- Core business logic
- Validation rules
- Edge cases and exception scenarios

#### Controller Layer
- API request and response handling
- Exception mapping
- Authorization and access control behavior



Framework-managed layers such as **repositories, entities, and configuration**
are intentionally lightweight and are not heavily unit-tested.

---

## Code Quality & Test Coverage

QuestEvent uses **JaCoCo** and **SonarQube** together to maintain code quality
and track test coverage.

### JaCoCo – Test Coverage

JaCoCo is integrated with the Maven build lifecycle to generate coverage reports
during test execution.

Coverage includes:
- Instruction coverage
- Branch coverage
- Line coverage
- Method coverage

#### Generate Coverage Report Locally

```bash
mvn clean test

