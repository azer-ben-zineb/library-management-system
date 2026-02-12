# LibraryPlus
**A modern, full-featured library management and book sales system built with JavaFX.**

## About
LibraryPlus isn't just another boring library app—it's a complete platform for borrowing, buying, and discovering books, wrapped in a beautiful interface. I built this because I wanted to see how far I could push JavaFX to look like a modern web app, complete with glassmorphism, smooth animations, and a rich feature set.

It solves the problem of clunky, outdated library systems by combining traditional lending mechanics with modern e-commerce features (like buying books and subscriptions) and social elements (ratings and reviews). Whether you're an admin managing stock or a user looking for your next read, it's designed to feel premium and intuitive.

## Features
**Core Functionality**
- [x] **Role-Based Access Control**: Distinct dashboards for Admins and Clients.
- [x] **Secure Authentication**: Salted BCrypt password hashing for robust security.
- [x] **Smart Dashboard**: dynamic "Featured Books" carousel and personalized welcome screens.
- [x] **Search & Discovery**: Real-time book search with category filtering and search history.

**Library & Sales**
- [x] **Lending System**: Borrow books, track due dates, and handle returns.
- [x] **Automated Fines**: Background service that calculates fines hourly for overdue books.
- [x] **Book Purchasing**: Buy books directly if you want to keep them forever.
- [x] **Stock Management**: Real-time tracking of inventory for both loans and sales.
- [x] **Waitlists**: Join a FIFO queue when a popular book is out of stock.

**User Experience**
- [x] **Subscription Tiers**: Standard vs. Premium memberships with different loan limits.
- [x] **Social Features**: Rate books and leave comments/reviews.
- [x] **AI Chatbot**: A built-in assistant to help you find books using natural language.
- [x] **Music Player**: Background ambient music with volume controls and mute toggle.
- [x] **Theming**: Beautiful Catppuccin Mocha theme applied throughout the app.
- [x] **Notifications**: Email reminders for due dates and an internal admin inbox.

## Tech Stack
| Technology | Purpose |
|------------|---------|
| **Java 21** | Core language (using latest features). |
| **JavaFX 21** | Modern UI framework for the desktop client. |
| **MySQL 8.1** | Robust relational database for production. |
| **H2 Database** | Embedded fallback database for zero-setup development. |
| **HikariCP** | High-performance JDBC connection pooling. |
| **JBcrypt** | Secure password hashing. |
| **Jakarta Mail** | Sending email notifications via SMTP. |
| **Logback / SLF4J** | Structured logging for debugging and audit trails. |
| **ControlsFX** | Extended UI controls for better user experience. |
| **Maven** | Dependency management and build automation. |

## Architecture & How It Works
The project follows a **Dao-Service-Controller** architecture, blending standard MVC with some MVP patterns for complex views.

### Structure
```
src/main/java/com/libraryplus
├── app         # Entry point (MainApplication) & Session management
├── ui          # JavaFX Controllers (View logic) & FXML wiring
├── service     # Business logic (Fines, Loans, Chatbot)
├── dao         # Data Access Objects (Direct generic JDBC implementations)
├── model       # POJOs representing DB entities (Book, User, Loan)
├── util        # Helpers (Audio, Themes, Security)
└── db          # Database connection factories
```

### The Flow
1.  **Request**: User clicks "Borrow" in the UI (`DashboardController`).
2.  **Controller**: The controller validates the input (e.g., is the user logged in?).
3.  **DAO Layer**: The controller calls `LoanDao` to insert a record.
4.  **Database**: The generic JDBC implementation executes SQL against MySQL (or H2).
5.  **Service**: Background services like `FineService` run independently to update state (calculating fines).

**Design Decisions:**
*   **Hybrid Database**: I implemented a smart fallback mechanism. The app tries to connect to MySQL first; if that fails, it seamlessly switches to an embedded H2 file database. This makes the app "just work" for testing without setting up a server.
*   **Separation of Concerns**: Services (like `ChatbotService`) are decoupled from the UI, so they can be tested independently or swapped out.
*   **Raw JDBC**: Instead of a heavy ORM like Hibernate, I used raw JDBC with `HikariCP`. For a desktop app of this size, it offers better performance and granular control over SQL queries.

## Implementation Highlights

### The "Self-Healing" Choice
The app is designed to be resilient. Logic in `MainApplication` detects the environment and configures the database accordingly, even injecting properties at runtime if needed.
```java
// MainApplication.java
try {
    String h2Url = "jdbc:h2:file:" + projectDir + "/data/libraryplus...";
    System.setProperty("H2_FILE_URL", h2Url); 
    // ... attempts to inject into environment map for child processes
} catch (Exception e) { /* ... */ }
```

### The AI Chatbot
The chatbot in `ChatbotService.java` isn't just a random string generator. It parses intent from your query to decide if you are searching by category, keyword, or just saying hello.
```java
// ChatbotService.java
if (lowerQuery.contains("show me") && lowerQuery.contains("books")) {
    String category = extractCategory(lowerQuery);
    if (category != null) {
        return findBooksByCategory(category);
    }
}
```

### Audio Integration
I wanted the app to feel immersive, so I built a persistent `AudioManager` singleton that plays background music across different scenes without restarting when you navigate.
```java
// AudioManager.java
public static AudioManager getInstance() {
    if (instance == null) instance = new AudioManager();
    return instance;
}
```

## Database
I use a relational schema with foreign keys to ensure data integrity.
*   **Core Tables**: `users`, `books`, `clients`
*   **Transactions**: `loans`, `purchases`, `transactions` (ledger)
*   **Social**: `ratings`, `comments`
*   **Logic**: `subscriptions`, `book_waitlist`

*See `src/main/resources/db/schema.sql` for the full definition.*

## API & Controllers
Since this is a desktop app, we don't have REST endpoints, but the **Controllers** act as the entry points for user actions.

| Controller | Feature Area | Key Actions |
|------------|--------------|-------------|
| `LoginController` | Auth | Authenticate user, load session |
| `DashboardController` | Main Hub | Search books, Toggle music, View featured |
| `AllBooksController` | Catalog | Grid view of all inventory |
| `AdminInboxController` | Admin | Read and reply to user messages |
| `SubscriptionViewController` | Membership | Upgrade/Downgrade plans |

## Prerequisites
*   **Java 21** or higher.
*   **Maven 3.8+**.
*   **MySQL Server 8.0+** (Optional, but recommended for production features).
*   **Internet Connection** (required for Maven dependencies on first run).

## How to Run

### Clone the repository
```bash
git clone https://github.com/yourusername/libraryplus.git
cd libraryplus
```

### Run with Maven
You can run the application directly without building a jar:
```bash
mvn clean javafx:run
```
*Note: The app will automatically detect your OS and download the correct JavaFX natives.*

### (Optional) Setup MySQL
If you want to use the full MySQL database instead of the file-based H2:
1. Create a database named `libraryplus`.
2. Update the `.env` or `db.properties` file with your credentials.
3. Run the valid SQL scripts in `src/main/resources/db/` to initialize tables.
