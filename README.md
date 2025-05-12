# 📚 LIBRIS - Library Management System

Libris is a full-featured library management system built with Spring Boot 3 and Java 21, developed for the **Patika.dev & Getir Java Spring Boot Bootcamp**. It offers RESTful APIs for managing books, users, borrow/return operations, and supports real-time features and scoring-based logic.

---

## 🔍 Features

### ✅ Core Features

- **Book Management**
    - Add, update, delete (soft delete), view, and search books by title, author, genre, ISBN
    - Pagination and case-insensitive search
- **User Management**
    - User registration and role-based control (GUEST, USER, LIBRARIAN, MANAGER)
    - CRUD operations for user data (with identity-based access rules)
- **Borrowing & Returning**
    - Borrow with due date and eligibility checks
    - Return tracking and overdue detection
    - Borrow history per user and global overdue reports
- **Authentication & Authorization**
    - Secure login/register using **Spring Security & JWT**
    - Role-based endpoint authorization
- **Documentation**
    - Fully documented with **Swagger/OpenAPI**
- **Logging**
    - Structured logging via **SLF4J + Logback**
    - Global exception handler with error detail logging
- **Testing**
    - **Unit & Integration Tests** using **JUnit + H2**
- **Postman Collection**
    - Includes all endpoint examples with test data

---

## 💎 Extra Features Implemented 

- ✅ **🧠 Dynamic Score & Level System**
    - Score-based classification (Novice → Bibliophile)
    - Rewards and penalties for user actions (early return, delay, loss, damage)
    - Borrowing rights and durations adapt based on level and score
- ✅ **📈 Statistics Dashboard**
    - Most borrowed books & genres
    - Overdue ratios
    - Monthly borrow trends
    - Average return durations
- ✅ **⚛️ Reactive Book Search**
    - Built using **Spring WebFlux**
    - Real-time availability stream with Observer Pattern
- ✅ **🔐 Identity Access Logic**
    - Users can only access/edit their own data
    - Librarians and managers have advanced control
- ✅ **🗂️ Soft Delete**
    - Instead of permanent deletion, records are flagged to preserve data history
- ✅ **🪪 Role-Permission Matrix**
    - Clear boundaries of what each role can perform in the system
- ✅ **🐳 Dockerized Deployment**
    - Application and PostgreSQL containers with `docker-compose.yml`
- ✅ **Audit Fields**
    - All entities track `createdAt`, `updatedAt`, and `deleted` status

---

## 🛠️ Tech Stack

| Layer        | Technology                      |
|--------------|----------------------------------|
| Backend      | Java 21, Spring Boot 3           |
| Reactive     | Spring WebFlux                   |
| Security     | Spring Security + JWT            |
| Database     | PostgreSQL (R2DBC + JPA)         |
| In-Memory DB | H2 (testing)                     |
| Testing      | JUnit 5, Mockito, Spring Test    |
| Documentation| Swagger / OpenAPI 3              |
| Logging      | SLF4J, Logback                   |
| Container    | Docker, Docker Compose           |
| Build Tool   | Maven                            |

---

## 📦 Installation

### Prerequisites

- Java 21+
- PostgreSQL
- Maven
- Docker (Optional)

### Clone and Run

```bash
git clone https://github.com/yourusername/libris.git
cd libris
mvn clean install
mvn spring-boot:run
## 🐳 Run with Docker

```bash
docker-compose up --build
```

> App runs on `http://localhost:8080`

---

## ⚙️ Configuration

- `application.yml` is configured for:
    - PostgreSQL (`localhost:5432/libris`, user: `postgres`, pass: `12345`)
    - R2DBC + JPA in separate packages
    - JWT secret, expiration time
    - Swagger URL: `http://localhost:8080/swagger-ui/index.html`

---

## 📊 API Endpoints Summary

### Auth

| Method | Endpoint              | Description       |
|--------|-----------------------|-------------------|
| POST   | `/api/auth/register`  | User registration |
| POST   | `/api/auth/login`     | User login        |

### Book

| Method | Endpoint                      | Role              |
|--------|-------------------------------|-------------------|
| GET    | `/api/books`                  | All               |
| GET    | `/api/books/{id}`             | All               |
| POST   | `/api/books`                  | LIBRARIAN, MANAGER |
| PUT    | `/api/books/{id}`             | LIBRARIAN, MANAGER |
| DELETE | `/api/books/{id}`             | LIBRARIAN, MANAGER |
| GET    | `/api/books/reactive/search`  | All (WebFlux)     |

### User

| Method | Endpoint            | Role           |
|--------|---------------------|----------------|
| GET    | `/api/users/{id}`   | SELF, LIBRARIAN |
| PUT    | `/api/users/{id}`   | SELF, LIBRARIAN |
| DELETE | `/api/users/{id}`   | LIBRARIAN      |

### Borrow

| Method | Endpoint                        | Role             |
|--------|----------------------------------|------------------|
| POST   | `/api/borrows`                   | USER             |
| POST   | `/api/borrows/return`            | USER             |
| GET    | `/api/borrows/history/{email}`   | SELF or LIBRARIAN |
| GET    | `/api/borrows/overdue`           | LIBRARIAN        |

### Statistics

| Method | Endpoint                                 | Role               |
|--------|------------------------------------------|--------------------|
| GET    | `/api/statistics/most-borrowed`          | LIBRARIAN, MANAGER |
| GET    | `/api/statistics/overdue-ratio`          | LIBRARIAN, MANAGER |
| GET    | `/api/statistics/monthly-borrow-counts`  | LIBRARIAN, MANAGER |
| GET    | `/api/statistics/average-return-time`    | LIBRARIAN, MANAGER |

> See Swagger for full request/response details.

---

## 🛡️ Role-Permission Matrix

| Role       | Book Operations       | User Operations      | Borrow Operations      | Statistics Access  |
|------------|------------------------|-----------------------|------------------------|--------------------|
| GUEST      | View/Search            | ❌                    | ❌                      | ❌                  |
| USER       | View/Search            | View/Update Self      | Borrow/Return/View Own | ❌                  |
| LIBRARIAN  | Full CRUD              | Full CRUD             | View All / Overdue     | ✅                  |
| MANAGER    | Full CRUD              | Full CRUD             | View All / Overdue     | ✅ (All statistics) |

> ✅ = full access, ❌ = no access

---
## 🧪 Testing

- Unit and integration test coverage for all services
- H2 used for isolated in-memory DB testing
- Swagger-tested via `/swagger-ui`

---

## 🔄 Postman Collection

🟢 Pre-configured Postman collection provided under:

```bash
/postman/libris-collection.json
```

Includes grouped folders for:

- Auth
- Books
- Users
- Borrow
- Statistics

---

## 📌 Database Schema

- `User` ⟶ `Borrow` (1-to-many)
- `Book` ⟶ `Borrow` (1-to-many)
- `Author` ⟶ `Book` (1-to-many)
- `Genre`: Enum
- `Level`: Enum (`NOVICE`, `READER`, `BOOKWORM`, `BIBLIOPHILE`)

> Refer to `/docs/schema.png` for the ER diagram.

---

## 📎 License

This project is for educational purposes only and is not licensed for commercial use.
