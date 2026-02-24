# Expense Tracker Backend

Spring Boot backend for the Expense Tracker application.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+

## Setup

1. Install PostgreSQL and create a database:
   ```sql
   CREATE DATABASE expense_tracker;
   ```

2. Update database credentials in `src/main/resources/application.properties`

3. Build the project:
   ```bash
   mvn clean install
   ```

4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The API will be available at `http://localhost:8080`

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
