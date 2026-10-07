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
Explain your application's architecture and major components.

## General Approach
Include a couple of paragraphs explaining:

How you approached the project.
How you structured your application.
How you implemented the major features.

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
While the Project is Running:
http://localhost:9091/swagger-ui/index.html

## Installation

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