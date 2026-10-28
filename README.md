# BookNest
is a streamlined library management platform designed to simplify the book loaning process.
The system bridges the gap between readers and library administrators by replacing manual tracking with a digital workflow that manages borrowing requests in real-time.

> **Project 3 update:** borrowing is now instant with a reviewed return, plus a waitlist, thread pools and race-condition handling. [Jump to the Project 3 Update](#project-3-update-concurrency)


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

## Project 3 Update: Concurrency

Project 3 extends the same application. The sections above describe the Project 2 version, so where they disagree with this section (the approval queue, the REQUESTED to APPROVED flow, the approve/reject endpoints), this section is the current behaviour.

### How the borrowing flow changed

**Before (Project 2):** a member sent a borrow request, it waited in the librarian's approval queue, and only after the librarian approved it did the book count as taken.

```
REQUESTED -> APPROVED -> returned          (or REJECTED)
```

**Now (Project 3):** borrowing is instant. The review moved from the start of the loan to the end, where it matters (is the book damaged, is the member late).

```
BORROWED -> PENDING_REVIEW -> RETURNED      (or BORROWED -> CANCELLED)
```

| Step | Who | What happens |
|------|-----|--------------|
| Borrow | Member | The loan starts right away as `BORROWED` and one copy is taken from the shelf. No approval needed. |
| Return | Member | The member hands the book back. The loan becomes `PENDING_REVIEW`. The system records if it was early, on time or late, and creates a late fine automatically (0.35 per day, `app.fines.late-per-day`). |
| Review | Librarian | The librarian checks the copy (`PUT /api/loans/{id}/review-return`). Not damaged: the copy goes back on the shelf, or is held for the next person in the waitlist. Damaged: the copy is removed from circulation (the total goes down) and the librarian can add a fine. |
| Cancel | Librarian | A loan that is still out (for example made by mistake) can be cancelled and the copy goes back. |

Why: an instant borrow means two members can really go for the last copy at the same moment, which is the concurrency problem this project is about.

### What is new

* **Waitlist with holds:** when a book has no copies, a member can join its waitlist (max 5 books per member). When a copy comes back it is **held for the first member in line** (24 hours, `app.holds.hours`) and is not put back on the shelf, so someone else cannot grab it. The member claims it, or the hold expires and the copy goes to the next person.
* **Fines:** late fines are created on return, damage fines by the librarian on review, and members pay them with `PUT /api/fines/{id}/pay`.
* **Live notifications (SSE):** new events `LOAN_RETURNED`, `RETURN_REVIEWED`, `LOAN_CANCELLED`, `HOLD_READY` and `HOLD_EXPIRED`.
* **Background work:** emails are sent in the background, a job expires old holds, and a daily job emails members whose book is due tomorrow.
* **New entity:** `WaitlistEntry` (`WAITING`, `HOLD_READY`, `FULFILLED`, `CANCELLED`, `EXPIRED`). The application now has 11 entities.

#### New and changed endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/loans` | Borrow a book, instant (`bookId`, `duration` 1-30 days) | User |
| PUT | `/api/loans/{loanId}/return` | Hand the book back, waits for review | User |
| GET | `/api/loans/returns-review` | Returns waiting for review | Librarian |
| PUT | `/api/loans/{loanId}/review-return` | Review a return (`damaged`, `notes`, `fineAmount`) | Librarian |
| PUT | `/api/loans/{loanId}/cancel` | Cancel a loan that is still out | Librarian |
| POST | `/api/waitlist` | Join the waitlist of a book | User |
| GET | `/api/waitlist/my` | My waitlist entries | User |
| GET | `/api/waitlist/{entryId}` | One entry with its position | User |
| DELETE | `/api/waitlist/{entryId}` | Leave the waitlist | User |
| POST | `/api/waitlist/{entryId}/claim?days=` | Claim the copy held for me | User |
| GET | `/api/waitlist/book/{bookId}` | The queue of a book | Librarian |
| GET | `/api/fines/my-fines` | My fines | User |
| PUT | `/api/fines/{id}/pay` | Pay a fine | User |
| GET | `/api/fines` | All fines | Librarian |
| GET | `/api/fines/user/{id}` | A user's fines | Librarian |
| POST | `/api/demo/race/{bookId}` | Race demo (only with the `race-demo` profile) | User |
| POST | `/api/demo/race/loan-limit/{username}` | Loan limit race demo (only with the `race-demo` profile) | User |

The old `approve` and `reject` endpoints no longer exist.

#### Business error codes (HTTP 409 unless noted)

| Code | Meaning |
|------|---------|
| `NO_COPIES_AVAILABLE` | Someone else took the last copy |
| `LOAN_LIMIT_REACHED` | The member already has 5 books on loan |
| `COPIES_AVAILABLE` | Tried to join a waitlist while copies are available, borrow instead |
| `WAITLIST_LIMIT_REACHED` | The member is already waiting for 5 books |
| `NO_ACTIVE_HOLD` | There is no hold to claim (already claimed, cancelled or not at the front yet) |
| `HOLD_EXPIRED` | The hold ran out before it was claimed |
| `FINE_ALREADY_PAID` | The fine was already paid |
| `CONCURRENT_UPDATE` | Two requests changed the same data at the same moment, try again (comes with a `Retry-After` header) |

### Race condition 1: the last copy of a book

**Shared resource:** the `inventory` row of a book (`availableCopies`).

**Why requests happen at the same time:** the server handles every HTTP request on its own thread. Two members can press Borrow on the same book in the same millisecond, and both requests read and change the same row.

**The race (book with 1 copy left):**

| Time | Member A | Member B |
|------|----------|----------|
| 1 | reads `availableCopies = 1` | |
| 2 | | reads `availableCopies = 1` |
| 3 | 1 > 0, ok. writes `0` | |
| 4 | | 1 > 0, ok (old value). writes `0` |
| 5 | loan created | loan created |

**Wrong result without control:** two loans for one physical copy, the stock is out of sync and the library has promised a book it does not have. With more threads the stock can even go negative.

**How it is prevented:** the borrow runs in a transaction and takes a **pessimistic write lock** on the inventory row (`SELECT ... FOR UPDATE`, `@Lock(PESSIMISTIC_WRITE)`). The second request waits at the lock until the first one commits, then reads the new value `0` and fails with `409 NO_COPIES_AVAILABLE`. Result: A gets `201 Created`, B gets `409 Conflict`.

**Why this approach:** the lock is done by the database, so it works for any number of threads and any number of app instances, which a Java `synchronized` block would not. Optimistic locking (`@Version`) would make the losers fail with errors and need retries, while with the lock they simply wait and then get a clear business answer.

**A detail that caused a real bug:** a book is loaded together with its inventory, so Hibernate can already hold an old copy of the inventory in memory when the lock is taken. The lock query then waits, but the object still shows the old numbers. `InventoryLocker` fixes this: it takes the lock and then re-reads the row (`entityManager.refresh`), so the check always uses the real stock. Every place that changes stock (`LoanService`, `WaitlistService`, `BookService`) takes the lock through this class.

**Demo:** with the `race-demo` profile, `POST /api/demo/race/{bookId}?safe=false` runs 10 threads at the last copy without the lock and shows the problem, and `safe=true` runs the real code and shows one success and nine rejections.

### Other races that are handled

| Situation | Problem without control | Prevention |
|-----------|-------------------------|------------|
| Same fine paid twice (double click or two devices) | Paid twice | The fine row is locked (`findWithLockById`). The second request sees `PAID` and gets `409 FINE_ALREADY_PAID`. |
| Member with 4 books borrows 4 more books at once | Ends up with more than 5 loans, because each request counted 4 | The member row is locked first, so the requests run one after another and only one passes the limit (`LOAN_LIMIT_REACHED`). |
| A walk-in tries to take a copy that was just returned for the waitlist | The first person in line loses their copy | A returned copy that is held is not added to `availableCopies`, so walk-ins get `NO_COPIES_AVAILABLE`. Only the member holding it can claim it. |
| Two librarians review the same return | The copy goes back on the shelf twice | The loan row is locked, the second review sees `RETURNED` and fails. |
| Hold expires at the same moment the member claims it or leaves | The copy is offered twice or lost | The waitlist entry is locked together with the book, and the status is re-checked after the lock. |
| Return and cancel of the same loan at once | Copy counted twice | The loan row is locked, only one of them can change `BORROWED`. |

**Lock order:** locks are always taken in the same order, **member first, then book**. If two requests took them in opposite orders they could wait for each other forever (a deadlock).

### Database consistency

The database should never be left wrong just because two requests overlapped. The rule that must always hold for every book:

```
totalCopies = availableCopies + copies on loan + copies held in the waitlist
availableCopies >= 0
```

How it is kept:
* every borrow, return, review, cancel, claim and hold expiry is one `@Transactional` operation, so either all of it is saved or none of it
* pessimistic row locks on inventory, member, fine, loan and waitlist entry (see above)
* notifications and emails are sent only **after the transaction commits** (`TransactionUtils.afterCommit`), so nobody is told about something that was rolled back
* lock failures are returned as `409 CONCURRENT_UPDATE` instead of a server error
* the tests check this rule after every concurrent scenario

### Multithreading used

| Where | What | Why |
|-------|------|-----|
| `notificationExecutor` (`AsyncConfig`) | `ThreadPoolTaskExecutor`, 4 to 8 threads, queue of 100, used with `@Async` in `AsyncEmailService` | The borrow request does not wait for the mail server |
| `holdExpiryExecutor` (`AsyncConfig`) | Fixed pool of 4 threads, `Callable` tasks, `invokeAll` with a 30 second timeout and `Future` results | `HoldExpiryJob` expires many holds at the same time and counts expired, skipped and failed |
| `DueReminderJob` | `CompletableFuture` for each email and `CompletableFuture.allOf` to wait for all of them | Sends the daily "due tomorrow" emails together |
| `@Scheduled` jobs | `HoldExpiryJob` every minute, `DueReminderJob` daily | Background processing |
| `RaceDemoService` | `ExecutorService` with a `CountDownLatch` start signal | Starts all threads at the same moment for the demo |

### Logging

The concurrent and background operations log what ran, the ids involved, how long it took and whether it worked (for example `Borrow started`, `Borrow completed ... in 12 ms`, `Hold expiry finished: 3 expired, 0 skipped, 0 failed`, email start and finish with the thread name). Refused requests and lock conflicts are logged as warnings. Passwords and tokens are never logged.

### Testing

Run with `mvn test`. The tests use an in-memory H2 database (`test` profile), so no PostgreSQL is needed, and the seeders are switched off in tests so each test creates its own data.

| Test class | What it covers |
|------------|----------------|
| `BookBorrowTest` | Borrow, return, review and cancel rules (unit tests with mocks) |
| `WaitlistServiceTest`, `ClaimHoldTest` | Join, hold, claim, expiry and leave rules, and the lock order |
| `ConcurrentBorrowTest` | 10 members for the last copy (1 wins, 9 get `NO_COPIES_AVAILABLE`), 3 copies for 10 members, the same member borrowing twice, the 5 book limit, return against cancel |
| `ConcurrentReturnAndFineTest` | The same fine paid 5 times, two librarians reviewing the same return |
| `ConcurrentWaitlistTest` | Positions when many join at once, a held copy against walk-ins, claiming and expiring the same hold at once |
| `HoldExpiryJobTest` | The thread pool expiring several holds together |
| `ConcurrentRestTest` | Real HTTP requests with JWT at the same time: one `201` and nine `409`, one fine payment `200` and four `409` |

Every concurrent test also checks the stock rule above.

### Updated planning

| Day | Work |
|-----|------|
| 1 | Waitlist model and workflow |
| 2 | Holds, claim endpoint, hold expiry |
| 3 | Race conditions found and fixed with locks, race demo |
| 4 | Thread pools, async emails, scheduled jobs |
| 5 | Logging, Swagger updates, cleanup |
| 6 | Concurrent tests, REST concurrency tests, test fixes |
| 7 | Documentation, user stories, final testing, presentation |

### Updated user stories

 * [Trello: user stories.](https://trello.com/invite/b/6ac5e7834bebef07fc444288/ATTIdecc791734b4cd6ac191843744f7f8216F35C90A/booknest)
