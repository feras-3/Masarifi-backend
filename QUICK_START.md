# Quick Start Guide

## TL;DR - Get Running in 2 Steps

### Step 1: Start the Backend

**Windows (PowerShell)**
```powershell
./start-server.ps1
```

**Mac/Linux**
```bash
./mvnw spring-boot:run -DskipTests
```

The application uses SQLite and will automatically create the database file (`expense_tracker.db`) on first run.

### Step 2: Test with Postman

1. Import `expense-tracker-postman-collection.json` into Postman
2. Run "Register User" request
3. Run "Login" request (token saves automatically)
4. Test other endpoints

Server runs at: `http://localhost:8080`

---

## Troubleshooting

### "JAVA_HOME not found" (Windows)
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-25"
```

### Database file location
The SQLite database file `expense_tracker.db` is created in the project root directory.

---

## Full Documentation

- [POSTMAN_TESTING_GUIDE.md](./POSTMAN_TESTING_GUIDE.md) - Complete API testing guide
- [expense-tracker-postman-collection.json](./expense-tracker-postman-collection.json) - Import into Postman

## Database Configuration

The application uses SQLite with the database file at `expense_tracker.db`. No additional database setup is required.
