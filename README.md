# Expense Tracker Backend

Spring Boot backend for the Expense Tracker application.

## Prerequisites

- Java 17 or higher
- Maven 3.6+

## Setup

1. Build the project:
   ```bash
   mvn clean install
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The API will be available at `http://localhost:8080`

The application uses SQLite as the database, which will automatically create an `expense_tracker.db` file in the project root directory on first run.

## Project Structure

- `controller/` - REST API endpoints
- `service/` - Business logic
- `repository/` - Data access layer
- `model/` - Entity classes
- `config/` - Application configuration
- `security/` - Security configuration

## Testing

Run tests with:
```bash
mvn test
```

Property-based tests use jqwik framework.
