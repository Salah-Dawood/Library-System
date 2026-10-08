# BookNest
is a streamlined library management platform designed to simplify the book loaning process.
The system bridges the gap between readers and library administrators by replacing manual tracking with a digital workflow that manages borrowing requests in real-time.


## Main features

### Authentication & Profile Management
* **User Authentication:** Secure user registration and login systems with role-based access control (Member vs. Librarian).
* **Password Recovery:** Self-service password reset workflow utilizing secure, time-sensitive email verification links.
* **Profile Customization:** Users can dynamically update their personal account information and contact details.
* **Profile Pictures:** Dedicated profile picture support allowing users to upload and change their profile photos.

### Request-Based Borrowing System
* **Borrow Requests:** Members browse the catalog and submit formal requests to borrow books.
* **Librarian Approval Queue:** An administrative dashboard where librarians review, accept, or reject pending requests.
* **Automatic Inventory Tracking:** System dynamically updates a book's availability status the moment a request is approved.

### Live Alerts & Communications
* **Live Notifications:** Real-time, instant in-app alerts that notify readers immediately when a librarian updates their request status.
* **Email Notifications:** Automated transactional emails sent to users for account password recovery and email verification upon registration.

### Returns, Reviews & History
* **Punctuality Tracking:** System logs whether a book was returned **On Time**, **Early**, or **Late** relative to its due date.
* **Member Return Records:** Maintains a history of return habits on each member's profile for administrative review.
* **Book Reviews:** Readers can leave ratings and feedback directly inside the system upon returning a borrowed book.
* **Return Processing:** Smooth workflow for checking books back into the active library inventory.

### System Security & Transparency
* **Audit Logs:** Comprehensive, chronological logs tracking system activity, request history, and administrative decisions for security.


## Technologies

### Backend Framework & Core
* **Java:** Core programming language.
* **Spring Boot:** Main application framework.
* **Maven / Gradle:** Build automation and dependency management tool.
* **Lombok:** Used to reduce boilerplate code (Getters, Setters, Builders) via annotations.

### Security & Performance
* **Spring Security:** Framework for robust authentication and role-based access control.
* **JSON Web Tokens (JWT):** Secured stateless session management.
* **Bucket4j:** API rate-limiting library implementation to prevent brute-force attacks and abuse.
* **Caffeine Cache:** In-memory caching engine used to support rate-limiting and cache application data.

### Database & Persistence
* **Spring Data JPA:** Abstracted data access layer utilizing Hibernate as the ORM provider.
* **PostgreSQL:** Reliable relational database used for production data storage.

### Communications & Templates
* **Spring Boot Starter Mail:** Core email infrastructure layer.
* **Mailtrap Java SDK:** Used as the secure email sending transport service and testing environment.
* **Thymeleaf:** Server-side Java template engine used to dynamically render HTML email templates.

### Live Connections & API Documentation
* **Server-Sent Events (SSE):** Utilized for pushing real-time request status updates to clients.
* **Springdoc OpenAPI / Swagger UI:** Automatically generated, interactive REST API documentation interface.


## Architecture
## System Architecture

The backend application follows a standard **layered architecture** design pattern to separate concerns, ensure maintainability, and enforce loose coupling. Request data flows linearly through the following layers:

### Supporting Components & Modules

* **Security Layer**
    * Implemented using **Spring Security** and stateless **JWT** interceptors.
    * Intercepts incoming requests to perform authentication, validate tokens, and enforce role-based authorization (Member vs. Librarian).
    * Integrates **Bucket4j** and **Caffeine Cache** directly into filter chains to enforce rate-limiting constraints per API user.

* **Data Transfer Objects (DTOs)**
    * Lightweight objects used to decouple internal database entities from external API payloads.
    * Prevents input into database that are not meant to be entered by users such as create/update time stamps
    * Prevents the exposure of sensitive database fields (like password hashes or auto-incremented keys) over public networks.


* **Exception Handling (`@RestControllerAdvice`)**
    * A centralized global exception handler that captures errors thrown anywhere within the application stack.
    * Translates custom errors (e.g., `BookNotAvailableException`, `InvalidTokenException`) into uniform, client-friendly JSON error structures containing consistent timestamps and status codes.

* **Server-Sent Events (SSE Functionality)**
    * Provides a lightweight, one-way asynchronous streaming connection between the server and the frontend client.
    * Enables the backend to immediately push live, real-time request status updates (in this case from REQUESTED to APPROVED) to the member's browser without requiring continuous polling.

* **Configuration Layer (`@Configuration`)**
    * Centralizes all Java-based system configurations.
    * Bootstraps beans for external services including **Spring Mail**, **Mailtrap Java SDK**, OpenApi/Swagger documentation setups, caching layers, and CORS filters.
    * Also when running for the first time or in an empty users table, the seeder seeds the database with initial users and other entities


## General Approach
Include a couple of paragraphs explaining:

### Approach
I started the project by implementing the user services such as registering and logging in and verifying email and so on.
That gave me a foundation to cuntinue working with the already implemented JWT security to restrict administrative services to tokens that include librarian role.
### Structure
I structure the app revolving around the book table and the system functionalities, this left the structure to always be scalable (ex. I initially did not have authors and reviews tables in mind).


## User Stories
[Trello: user stories.](https://trello.com/invite/b/6ac5e7834bebef07fc444288/ATTIdecc791734b4cd6ac191843744f7f8216F35C90A/booknest)

## ERD
[ERD](https://github.com/Salah-Dawood/Library-System/blob/main/Library-System.png)

### Deliverables
Java Spring boot project

Bonus: frontend utilizing Java backend (**AI GENERATED**)

### Timeline
7 Day Project
* **Day 1:** planning architecture 
* **Day 2:** initializing project & creating authentication architecture
* **Day 3:** implementing required models
* **Day 4:** implementing endpoints and business logic
* **Day 5:** implementing technical requirements (SSE,Seeding,Logging,Rate limting)
* **Day 6:** implementing technical requirements (pagination,sorting,filteration,swagger)
* **Day 7:** apply testing, write documentation

### API Documentation
#### Endpoints

**Auth legend:** 🌐 Public · 🔑 Any logged-in user (JWT) · 📚 Librarian only

##### Authentication & Email (`/auth`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/auth/users/register` | Register a new member | 🌐 |
| POST | `/auth/users/login` | Log in and receive a JWT | 🌐 |
| PUT | `/auth/users/passwordreset/{token}` | Reset password using emailed token | 🌐 |
| PUT | `/auth/email/getverification/{username}` | Send email verification code | 🌐 |
| PUT | `/auth/email/verify/{username}?code=` | Verify email with code | 🌐 |
| PUT | `/auth/email/forgotpassword` | Send password-reset email | 🌐 |

##### Books (`/api/books`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/books` | List books (paged). Params: `title`, `genre`, `page`, `size`, `sortBy`, `sortDir` | 🔑 |
| GET | `/api/books/{bookId}` | Get one book with stock and rating | 🔑 |
| GET | `/api/books/search/{title}` | Get a book by exact title | 🔑 |
| POST | `/api/books` | Create a book | 📚 |
| PUT | `/api/books/{bookId}` | Update a book | 📚 |
| DELETE | `/api/books/{bookId}` | Delete a book | 📚 |

##### Authors (`/api/authors`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/authors` | List all authors | 🔑 |
| GET | `/api/authors/{authorId}` | Get author by id | 🔑 |
| GET | `/api/authors/search/{name}` | Get author by exact name | 🔑 |
| POST | `/api/authors` | Create an author | 📚 |
| PUT | `/api/authors/{authorId}` | Update an author | 📚 |
| DELETE | `/api/authors/{authorId}` | Delete an author | 📚 |

##### Genres (`/api/genres`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/genres` | List all genres | 🔑 |
| GET | `/api/genres/{name}` | Get genre by exact name | 🔑 |
| POST | `/api/genres` | Create a genre | 📚 |
| PUT | `/api/genres/{genreId}` | Update a genre | 📚 |
| DELETE | `/api/genres/{genreId}` | Delete a genre | 📚 |

##### Loans (`/api/loans`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/loans` | Request a book loan (`bookId`, `duration` 1-30 days) | 🔑 |
| GET | `/api/loans/my-loans` | List my loans | 🔑 |
| GET | `/api/loans/my-stats` | My return statistics and reliability badge | 🔑 |
| PUT | `/api/loans/{loanId}/cancel` | Cancel a loan (owner: requested only; librarian: also approved) | 🔑 |
| PUT | `/api/loans/{loanId}/return` | Return a borrowed book (owner or librarian) | 🔑 |
| GET | `/api/loans?status=` | List all loans, optional status filter | 📚 |
| GET | `/api/loans/user/{userId}` | List a user's loans | 📚 |
| GET | `/api/loans/user/{userId}/stats` | A user's return statistics | 📚 |
| PUT | `/api/loans/{loanId}/approve` | Approve a loan request | 📚 |
| PUT | `/api/loans/{loanId}/reject` | Reject a loan request (optional `reason`) | 📚 |

##### Reviews (`/api`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/books/{bookId}/reviews` | Get reviews and rating summary for a book | 🔑 |
| POST | `/api/books/{bookId}/reviews` | Add a review (`rating`, `comment`) | 🔑 |
| PUT | `/api/reviews/{reviewId}` | Update a review | 🔑 |
| DELETE | `/api/reviews/{reviewId}` | Delete a review | 🔑 |

##### Member Profile (`/api/member`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/member/profile` | Get my profile | 🔑 |
| PUT | `/api/member/update/profile` | Update my profile (multipart, optional image) | 🔑 |
| PUT | `/api/member/change-password` | Change my password | 🔑 |

##### Librarian Admin (`/api/librarian`, `/api/audit-log`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/librarian/users` | List all users | 📚 |
| GET | `/api/librarian/users/{userId}` | Get user details | 📚 |
| PUT | `/api/librarian/users/deactivate/{userId}` | Deactivate a user | 📚 |
| PUT | `/api/librarian/users/activate/{userId}` | Activate a user | 📚 |
| GET | `/api/audit-log?type=&userId=` | View audit log (`USER`, `BOOK`, `LOAN`, `REVIEW`) | 📚 |

##### Notifications

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/notifications/stream` | Real-time notifications (Server-Sent Events) | 🔑 |

#### Swagger
http://localhost:9091/swagger-ui/index.html


## Installation (how to run)
Follow these steps to configure your IntelliJ environment and run the BookNest system locally.

### Prerequisites
Before running the application, ensure you have the following ready:
* **Java Development Kit (JDK) 17 or higher** installed.
* **IntelliJ IDEA** (Community or Ultimate edition).
* **PostgreSQL** server running with an empty database created, with the name booknest-dev or booknest-demo
* A **Mailtrap** account for email testing (if using dev profile)
* A **Email account** with an app password (if using demo profile)

### Configure IntelliJ Environment Variables
configure the following environment variables that will be used by application.properties

#### General
* **DB_PASSWORD:** your PostgreSQL password
* **JWT_SECRET:** This must be a secure, random string of at least 64 characters (512 bits)

#### Demo Profile
* **DEMO_DATABASE:** link to PostgreSQL Database
* **EMAIL_USERNAME:** an email the app will use as source(ex. johndoe@gmail.com)
* **EMAIL_PASSWORD:** email app password for the email (not normal password email) 16 characters long

#### Dev Profile
* **DEV_DATABASE:** link to PostgreSQL Database
* **MAILTRAP_USERNAME:** mailtrap user name, can be seen in mailtrap dashboard. **without domain**
* **MAILTRAP_PASSWORD:** mailtrap password, can be seen in mailtrap dashboard.


## Unsolved Problems
Some requirements implemented are not system system wide, rather only implemented once to show understanding of using the feature.
## Major Challenges
Had a small problem configuring a Many to Many relationship between two entities for the first time, but i found it nice that java/spring boot handles making the junction table.
## Future Improvements
**Spread the requirements** accross the system where it would be appropriate and efficient.
**Implement email notification** for loan status updates

## Credits & External Resources
* **URL:** [Claude](https://claude.ai)
* **Purpose:** Built a clean, simple frontend interface for the project demo.

* **URL:** [Google AI Mode](https://ai.google)
* **Purpose:** Provided end-to-end architectural guidance, explained complex Java structures, and mentored on industry best practices throughout development.