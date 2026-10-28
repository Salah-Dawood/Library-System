# BookNest
is a streamlined library management platform designed to simplify the book loaning process.
The system bridges the gap between readers and library administrators by replacing manual tracking with a digital workflow that manages borrowing requests in real-time.

> **Project 3 update:** borrowing is now instant with a reviewed return, plus a waitlist, thread pools and race-condition handling. [Jump to the Project 3 Update](#project-3-multithreading-and-thread-safety-update)


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

**Auth legend:** Public = no token needed · User = any logged-in user (JWT) · Librarian = librarian only

##### Authentication & Email (`/auth`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/auth/users/register` | Register a new member | Public |
| POST | `/auth/users/login` | Log in and receive a JWT | Public |
| PUT | `/auth/users/passwordreset/{token}` | Reset password using emailed token | Public |
| PUT | `/auth/email/getverification/{username}` | Send email verification code | Public |
| PUT | `/auth/email/verify/{username}?code=` | Verify email with code | Public |
| PUT | `/auth/email/forgotpassword` | Send password-reset email | Public |

##### Books (`/api/books`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/books` | List books (paged). Params: `title`, `genre`, `page`, `size`, `sortBy`, `sortDir` | User |
| GET | `/api/books/{bookId}` | Get one book with stock and rating | User |
| GET | `/api/books/search/{title}` | Get a book by exact title | User |
| POST | `/api/books` | Create a book | Librarian |
| PUT | `/api/books/{bookId}` | Update a book | Librarian |
| DELETE | `/api/books/{bookId}` | Delete a book | Librarian |

##### Authors (`/api/authors`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/authors` | List all authors | User |
| GET | `/api/authors/{authorId}` | Get author by id | User |
| GET | `/api/authors/search/{name}` | Get author by exact name | User |
| POST | `/api/authors` | Create an author | Librarian |
| PUT | `/api/authors/{authorId}` | Update an author | Librarian |
| DELETE | `/api/authors/{authorId}` | Delete an author | Librarian |

##### Genres (`/api/genres`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/genres` | List all genres | User |
| GET | `/api/genres/{name}` | Get genre by exact name | User |
| POST | `/api/genres` | Create a genre | Librarian |
| PUT | `/api/genres/{genreId}` | Update a genre | Librarian |
| DELETE | `/api/genres/{genreId}` | Delete a genre | Librarian |

##### Loans (`/api/loans`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/loans` | Request a book loan (`bookId`, `duration` 1-30 days) | User |
| GET | `/api/loans/my-loans` | List my loans | User |
| GET | `/api/loans/my-stats` | My return statistics and reliability badge | User |
| PUT | `/api/loans/{loanId}/cancel` | Cancel a loan (owner: requested only; librarian: also approved) | User |
| PUT | `/api/loans/{loanId}/return` | Return a borrowed book (owner or librarian) | User |
| GET | `/api/loans?status=` | List all loans, optional status filter | Librarian |
| GET | `/api/loans/user/{userId}` | List a user's loans | Librarian |
| GET | `/api/loans/user/{userId}/stats` | A user's return statistics | Librarian |
| PUT | `/api/loans/{loanId}/approve` | Approve a loan request | Librarian |
| PUT | `/api/loans/{loanId}/reject` | Reject a loan request (optional `reason`) | Librarian |

##### Reviews (`/api`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/books/{bookId}/reviews` | Get reviews and rating summary for a book | User |
| POST | `/api/books/{bookId}/reviews` | Add a review (`rating`, `comment`) | User |
| PUT | `/api/reviews/{reviewId}` | Update a review | User |
| DELETE | `/api/reviews/{reviewId}` | Delete a review | User |

##### Member Profile (`/api/member`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/member/profile` | Get my profile | User |
| PUT | `/api/member/update/profile` | Update my profile (multipart, optional image) | User |
| PUT | `/api/member/change-password` | Change my password | User |

##### Librarian Admin (`/api/librarian`, `/api/audit-log`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/librarian/users` | List all users | Librarian |
| GET | `/api/librarian/users/{userId}` | Get user details | Librarian |
| PUT | `/api/librarian/users/deactivate/{userId}` | Deactivate a user | Librarian |
| PUT | `/api/librarian/users/activate/{userId}` | Activate a user | Librarian |
| GET | `/api/audit-log?type=&userId=` | View audit log (`USER`, `BOOK`, `LOAN`, `REVIEW`) | Librarian |

##### Notifications

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/notifications/stream` | Real-time notifications (Server-Sent Events) | User |

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

---

# Project 3: Multithreading and Thread Safety Update

Project 3 builds on the same app. Where the Project 2 sections above disagree with this update (the approval queue and the approve/reject endpoints), this update is the current behaviour.

## Project 3 Changes

**New functionality**
* Borrowing is now instant. The librarian check moved to the end of the loan: the member returns the book, then a librarian reviews it for damage.
* Waitlist: if a book has no copies, members can join its waitlist. When a copy comes back it is held for the first person in line for 24 hours.
* Fines: late fines are created automatically on return, damage fines by the librarian. Members pay them.
* Background jobs: emails are sent in the background, holds are expired every minute and a daily job reminds members about books due tomorrow.
* Race demo (`race-demo` profile) and a new frontend with a Race lab page and an audit log page for librarians.

**New models**
* `WaitlistEntry`: book, user, status (`WAITING`, `HOLD_READY`, `FULFILLED`, `CANCELLED`, `EXPIRED`), hold expiry time and join time.
* `Loan` has the new statuses `BORROWED`, `PENDING_REVIEW`, `RETURNED`, `CANCELLED` and review fields. `Fine` is linked to a loan and has a type and a status.

**Concurrency-sensitive workflow:** borrow, return, review, cancel, claim a held copy, hold expiry and pay a fine. All of them change the number of copies of a book or the state of a loan, and can run at the same time as the same action from another user.

**Race condition:** two members borrowing the last copy at the same moment (more in the table below).

**Concurrency technique:** pessimistic locking (`@Lock(PESSIMISTIC_WRITE)`, which is `SELECT ... FOR UPDATE`) inside `@Transactional` methods.

**Database consistency:** every operation is one transaction, and this rule must always hold for a book: `total copies = copies on the shelf + copies on loan + copies held for the waitlist`. Emails and notifications are sent only after the transaction commits.

**Asynchronous processing**
* `notificationExecutor`: thread pool for `@Async` emails, so a request does not wait for the mail server.
* `holdExpiryExecutor`: fixed pool with `Callable` and `Future`, used by the job that expires holds.
* `DueReminderJob`: `CompletableFuture.allOf` for the daily reminder emails.

## Concurrency Explanation

**Where it happens:** the server handles each request on its own thread, so two members can borrow the same book at the same time. The hold-expiry job also runs at the same time as members.

**What goes wrong without control** (1 copy left):

| Time | Member A | Member B |
|------|----------|----------|
| 1 | reads `availableCopies = 1` | |
| 2 | | reads `availableCopies = 1` |
| 3 | writes `0`, loan created | |
| 4 | | still thinks it is 1, writes `0`, loan created |

Two loans for one copy, and the stock is wrong.

**How it is prevented:** the borrow locks the inventory row first. B waits until A commits, then sees `0` and gets `409 NO_COPIES_AVAILABLE`. One lock helper, `InventoryLocker`, is used everywhere stock changes. It also re-reads the row after locking, because Hibernate can hold an old copy in memory.

**Why this approach:** the database does the locking, so it works with any number of threads or servers (a Java `synchronized` would not). Waiting is simpler than optimistic locking, where the losers would fail and need a retry. Locks are always taken member first, then book, so two requests cannot deadlock.

**Race conditions handled**

| Situation | Without control | Prevented by |
|-----------|-----------------|--------------|
| Many members borrow the last copy | More loans than copies | Lock on the inventory row |
| One member borrows many books at once | More than 5 loans | Lock on the member row first |
| Same fine paid twice | Paid twice | Lock on the fine, second gets `FINE_ALREADY_PAID` |
| Two librarians review the same return | Copy put back twice | Lock on the loan |
| Walk-in takes a copy held for the waitlist | First in line loses it | Held copies are not counted as available |
| Hold expires while the member claims it | Copy lost or offered twice | Lock on the entry and the book, status checked again |

## Testing

Tests run with `mvn test` on an in-memory H2 database (`test` profile). For the concurrent tests, a helper (`ConcurrentRunner`) starts all threads together with a `CountDownLatch` so they hit the code at the same moment, then the test checks the results and the stock rule.

```java
List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);   // 10 members, 1 copy
assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
assertEquals(9, ConcurrentRunner.countErrorCode(outcomes, "NO_COPIES_AVAILABLE"));
data.assertStockConsistent(book);
```

* `ConcurrentBorrowTest`: last copy, loan limit, return against cancel.
* `ConcurrentReturnAndFineTest`: same fine paid 5 times, two librarians reviewing one return.
* `ConcurrentWaitlistTest`, `HoldExpiryJobTest`: held copies and hold expiry.
* `ConcurrentRestTest`: real HTTP requests at the same time, one `201` and nine `409`.
* `BookBorrowTest`, `WaitlistServiceTest`, `ClaimHoldTest`: normal rules with mocks.

## Updated API

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/loans` | Borrow a book (now instant) | User |
| PUT | `/api/loans/{id}/return` | Return a book, waits for review | User |
| GET | `/api/loans/returns-review` | Returns waiting for review | Librarian |
| PUT | `/api/loans/{id}/review-return` | Review a return (`damaged`, `notes`, `fineAmount`) | Librarian |
| PUT | `/api/loans/{id}/cancel` | Cancel a loan that is still out | Librarian |
| POST | `/api/waitlist` | Join a waitlist | User |
| GET | `/api/waitlist/my` | My waitlist entries | User |
| GET | `/api/waitlist/{id}` | One entry with its position | User |
| DELETE | `/api/waitlist/{id}` | Leave the waitlist | User |
| POST | `/api/waitlist/{id}/claim?days=` | Claim the held copy | User |
| GET | `/api/waitlist/book/{bookId}` | Queue of a book | Librarian |
| GET | `/api/fines/my-fines` | My fines | User |
| PUT | `/api/fines/{id}/pay` | Pay a fine | User |
| GET | `/api/fines` | All fines | Librarian |
| GET | `/api/fines/user/{id}` | A user's fines | Librarian |
| POST | `/api/demo/race/{bookId}?threads=&safe=` | Last copy race demo | Librarian |
| POST | `/api/demo/race/loan-limit/{username}?threads=&safe=` | Loan limit race demo | Librarian |

The old approve and reject endpoints were removed. Conflicts return `409` with a code such as `NO_COPIES_AVAILABLE`, `LOAN_LIMIT_REACHED`, `NO_ACTIVE_HOLD`, `HOLD_EXPIRED`, `FINE_ALREADY_PAID` or `CONCURRENT_UPDATE`.

## Project 3 Challenges

* The first lock still let a second borrow through, because Hibernate had already loaded an old copy of the stock. I fixed it by re-reading the row after locking (`InventoryLocker`). The race demo had a similar problem and needed `entityManager.clear()`.
* Deciding what to lock, and in which order, so that requests cannot deadlock.
* A held copy must not count as available, otherwise walk-ins take it.
* Races do not fail every time, so the tests needed a latch to start threads together and repeated runs.

## Project 3 Future Improvements

* Run the tests on PostgreSQL (for example with Testcontainers), because they use H2.
* Retry automatically on `CONCURRENT_UPDATE` and add a lock timeout.
* Make sure only one server instance runs the scheduled jobs.

## Project 3 Credits & External Resources

These continue the credits from Project 2 above.

* **Claude** - [https://claude.ai](https://claude.ai) - AI help to plan Project 3, explain locking and thread pools, and review the code, As well as writing the final frontend.
* **Google AI** - General/Quick search about related topics.

Thanks to instructor associates: Maryam and Zainab for technical help.