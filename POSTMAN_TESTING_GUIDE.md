# Postman Testing Guide for Expense Tracker API

This guide will help you test the Expense Tracker backend API using Postman.

## Prerequisites

### 1. Install and Start PostgreSQL Database

The application requires PostgreSQL to be running. Choose one option:

#### Option A: Install PostgreSQL Locally
1. Download PostgreSQL from [postgresql.org/download](https://www.postgresql.org/download/)
2. Install and start the PostgreSQL service
3. Create the database:
   ```sql
   CREATE DATABASE expense_tracker;
   ```
4. Update credentials in `application.properties` if needed

#### Option B: Use Docker (Recommended for Testing)
```bash
docker run --name expense-tracker-db -e POSTGRES_PASSWORD=password -e POSTGRES_DB=expense_tracker -p 5432:5432 -d postgres:15
```

To stop the database later:
```bash
docker stop expense-tracker-db
```

To start it again:
```bash
docker start expense-tracker-db
```

#### Verify PostgreSQL is Running
Check if PostgreSQL is accessible:
```bash
# On Windows (PowerShell)
Test-NetConnection -ComputerName localhost -Port 5432

# On Mac/Linux
nc -zv localhost 5432
```

### 2. Set JAVA_HOME (Windows Only)

If you get "JAVA_HOME not found" error on Windows:

```powershell
# Set for current session
$env:JAVA_HOME="C:\Program Files\Java\jdk-25"

# Or set permanently (run as Administrator)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-25", "Machine")
```

### 3. Start the Backend Server
   
On Mac/Linux:
```bash
cd Masarifi-backend/Masarifi-backend
./mvnw spring-boot:run -DskipTests
```

On Windows (PowerShell):
```powershell
cd Masarifi-backend/Masarifi-backend
$env:JAVA_HOME="C:\Program Files\Java\jdk-25"
./mvnw spring-boot:run -DskipTests
```

On Windows (CMD):
```cmd
cd Masarifi-backend\Masarifi-backend
set JAVA_HOME=C:\Program Files\Java\jdk-25
mvnw.cmd spring-boot:run -DskipTests
```

The server will start on `http://localhost:8080`

Note: We use `-DskipTests` to skip running tests during startup for faster development

### 4. Postman Installation
- Download from [postman.com](https://www.postman.com/downloads/)

## Base URL

```
http://localhost:8080
```

## API Endpoints Overview

### 1. Authentication Endpoints (No Auth Required)

#### 1.1 Register User
- **Method**: POST
- **URL**: `http://localhost:8080/api/auth/register`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "username": "testuser",
    "password": "password123"
  }
  ```
- **Expected Response** (200 OK):
  ```json
  "User registered successfully"
  ```

#### 1.2 Login
- **Method**: POST
- **URL**: `http://localhost:8080/api/auth/login`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "username": "testuser",
    "password": "password123"
  }
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400000
  }
  ```
- **Important**: Copy the `token` value - you'll need it for all subsequent requests!

---

### 2. Transaction Endpoints (Auth Required)

For all endpoints below, add this header:
```
Authorization: Bearer YOUR_JWT_TOKEN_HERE
```

#### 2.1 Get All Transactions
- **Method**: GET
- **URL**: `http://localhost:8080/api/transactions`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  [
    {
      "id": "uuid",
      "amount": 50.00,
      "category": "FOOD",
      "description": "Grocery shopping",
      "date": "2024-01-15",
      "userId": "testuser"
    }
  ]
  ```

#### 2.2 Create Transaction
- **Method**: POST
- **URL**: `http://localhost:8080/api/transactions`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "amount": 50.00,
    "category": "Food",
    "description": "Grocery shopping",
    "date": "2024-01-15"
  }
  ```
- **Valid Categories** (case-sensitive):
  - `Food`
  - `Transportation`
  - `Entertainment`
  - `Utilities`
  - `Healthcare`
  - `Shopping`
  - `Other`
  
- **Expected Response** (201 Created):
  ```json
  {
    "id": "generated-uuid",
    "amount": 50.00,
    "category": "Food",
    "description": "Grocery shopping",
    "date": "2024-01-15",
    "userId": "testuser",
    "source": "MANUAL",
    "createdAt": "2024-01-15T10:30:00"
  }
  ```

- **Common Errors**:
  - Invalid category: Use exact capitalization (e.g., "Food" not "FOOD" or "food")
  - Date in future: Date must be today or in the past
  - Missing fields: All fields are required

#### 2.3 Update Transaction
- **Method**: PUT
- **URL**: `http://localhost:8080/api/transactions/{id}`
  - Replace `{id}` with actual transaction ID
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "amount": 75.00,
    "category": "FOOD",
    "description": "Updated grocery shopping",
    "date": "2024-01-15"
  }
  ```

#### 2.4 Delete Transaction
- **Method**: DELETE
- **URL**: `http://localhost:8080/api/transactions/{id}`
  - Replace `{id}` with actual transaction ID
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "success": true
  }
  ```

---

### 3. Budget Endpoints (Auth Required)

#### 3.1 Get All Budgets
- **Method**: GET
- **URL**: `http://localhost:8080/api/budgets/current`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  [
    {
      "id": "uuid-1",
      "amount": 500.00,
      "spent": 150.00,
      "remaining": 350.00,
      "percentageUsed": 30.00,
      "period": "2026-02"
    },
    {
      "id": "uuid-2",
      "amount": 600.00,
      "spent": 200.00,
      "remaining": 400.00,
      "percentageUsed": 33.33,
      "period": "2026-01"
    }
  ]
  ```
- **Error Response** (404 Not Found) - No budgets exist:
  ```json
  {
    "error": "Not found",
    "message": "User doesn't have any budgets yet"
  }
  ```

#### 3.2 Get Budget by ID
- **Method**: GET
- **URL**: `http://localhost:8080/api/budgets/{id}`
  - Replace `{id}` with actual budget ID
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "id": "uuid",
    "amount": 500.00,
    "spent": 150.00,
    "remaining": 350.00,
    "percentageUsed": 30.00,
    "period": "2026-02"
  }
  ```

#### 3.3 Create Budget
- **Method**: POST
- **URL**: `http://localhost:8080/api/budgets`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "amount": 500.00,
    "period": "MONTHLY"
  }
  ```
  Or specify exact month:
  ```json
  {
    "amount": 500.00,
    "period": "2026-02"
  }
  ```
- **Period Options**:
  - `MONTHLY` - Creates budget for current month (recommended)
  - `YYYY-MM` - Specific month (e.g., "2026-02" for February 2026)
  - `WEEKLY`, `DAILY`, `YEARLY` - Also supported (uses current month)
  
- **Important**: Creating a budget with the same period will overwrite the existing budget for that period
  
- **Expected Response** (201 Created):
  ```json
  {
    "id": "uuid",
    "userId": "testuser",
    "amount": 500.00,
    "period": "2026-02",
    "createdAt": "2026-02-26T10:30:00",
    "updatedAt": "2026-02-26T10:30:00"
  }
  ```

#### 3.4 Update Budget
- **Method**: PUT
- **URL**: `http://localhost:8080/api/budgets/{id}`
  - Replace `{id}` with actual budget ID
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "amount": 600.00
  }
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "id": "uuid",
    "userId": "testuser",
    "amount": 600.00,
    "period": "2026-02",
    "createdAt": "2026-02-26T10:30:00",
    "updatedAt": "2026-02-26T11:00:00"
  }
  ```

---#### 3.1 Get All Budgets
- **Method**: GET
- **URL**: `http://localhost:8080/api/budgets/current`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "id": "uuid",
    "amount": 500.00,
    "spent": 150.00,
    "remaining": 350.00,
    "percentageUsed": 30.00,
    "period": "2026-02"
  }
  ```
- **Error Response** (404 Not Found) - No budget exists:
  ```json
  {
    "error": "Not found",
    "message": "User doesn't have a budget yet"
  }
  ```

#### 3.2 Create Budget
- **Method**: POST
- **URL**: `http://localhost:8080/api/budgets`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "category": "FOOD",
    "amount": 500.00,
    "period": "MONTHLY"
  }
  ```
- **Expected Response** (201 Created):
  ```json
  {
    "id": "uuid",
    "category": "FOOD",
    "amount": 500.00,
    "spent": 0.00,
    "remaining": 500.00,
    "period": "MONTHLY",
    "status": "UNDER_BUDGET"
  }
  ```

#### 3.3 Update Budget
- **Method**: PUT
- **URL**: `http://localhost:8080/api/budgets/{id}`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "category": "FOOD",
    "amount": 600.00,
    "period": "MONTHLY"
  }
  ```

#### 3.4 Delete Budget
- **Method**: DELETE
- **URL**: `http://localhost:8080/api/budgets/{id}`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```

---

### 4. Plaid Integration Endpoints (Auth Required)

#### 4.1 Create Public Token
- **Method**: POST
- **URL**: `http://localhost:8080/api/plaid/public-token`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "institutionId": "ins_109508"
  }
  ```
- **Request Parameters**:
  - `institutionId` (required): The sandbox institution ID to link
  
- **Common Institution IDs** (Sandbox):
  - `ins_109508` - First Platypus Bank (default)
  - `ins_109509` - First Gingham Credit Union
  - `ins_109510` - Tattersall Federal Credit Union
  - `ins_109511` - Tartan Bank
  
- **Expected Response** (200 OK):
  ```json
  {
    "publicToken": "public-sandbox-xxxxx-xxxxx"
  }
  ```
  
- **Note**: This endpoint does NOT require authentication (no Bearer token needed). It uses Plaid credentials configured in `application.properties` (plaid.client.id and plaid.secret).

#### 4.2 Exchange Public Token
- **Method**: POST
- **URL**: `http://localhost:8080/api/plaid/link-token`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "linkToken": "link-sandbox-xxxxx"
  }
  ```

#### 4.2 Exchange Public Token
- **Method**: POST
- **URL**: `http://localhost:8080/api/plaid/exchange-token`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  Content-Type: application/json
  ```
- **Body** (raw JSON):
  ```json
  {
    "publicToken": "public-sandbox-xxxxx"
  }
  ```
- **Expected Response** (201 Created):
  ```json
  {
    "success": true,
    "accountId": "uuid",
    "institutionName": "Chase"
  }
  ```

#### 4.3 Get Linked Accounts
- **Method**: GET
- **URL**: `http://localhost:8080/api/plaid/accounts`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "accounts": [
      {
        "id": "uuid",
        "institutionName": "Chase",
        "isActive": true
      }
    ],
    "linked": true
  }
  ```

#### 4.4 Sync Transactions
- **Method**: POST
- **URL**: `http://localhost:8080/api/plaid/sync-transactions`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "added": 5,
    "modified": 2,
    "removed": 0,
    "success": true
  }
  ```

#### 4.5 Unlink Account
- **Method**: DELETE
- **URL**: `http://localhost:8080/api/plaid/unlink`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "success": true
  }
  ```

---

### 5. Alert Endpoints (Auth Required)

#### 5.1 Get All Alerts
- **Method**: GET
- **URL**: `http://localhost:8080/api/alerts`
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "alerts": [
      {
        "id": "uuid",
        "message": "Budget exceeded for FOOD",
        "type": "BUDGET_EXCEEDED",
        "isRead": false,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "unreadCount": 1
  }
  ```

#### 5.2 Dismiss Alert
- **Method**: PUT
- **URL**: `http://localhost:8080/api/alerts/{id}/dismiss`
  - Replace `{id}` with actual alert ID
- **Headers**: 
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```
- **Expected Response** (200 OK):
  ```json
  {
    "success": true
  }
  ```

---

## Setting Up Postman Collection

### Option 1: Manual Setup

1. **Create a New Collection**
   - Click "New" → "Collection"
   - Name it "Expense Tracker API"

2. **Set Collection Variables**
   - Click on the collection → "Variables" tab
   - Add variables:
     - `baseUrl`: `http://localhost:8080`
     - `token`: (leave empty, will be set after login)

3. **Create Requests**
   - Add folders: "Auth", "Transactions", "Budgets", "Plaid", "Alerts"
   - Add requests as documented above
   - Use `{{baseUrl}}` and `{{token}}` variables

4. **Set Authorization**
   - For authenticated endpoints, use: `Bearer {{token}}`

### Option 2: Import Collection (Recommended)

Save the following as `expense-tracker-postman-collection.json`:

```json
{
  "info": {
    "name": "Expense Tracker API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    },
    {
      "key": "token",
      "value": ""
    }
  ]
}
```

Then import it into Postman: File → Import → Select file

---

## Testing Workflow

### Complete Test Flow

1. **Register a new user**
   ```
   POST /api/auth/register
   ```

2. **Login**
   ```
   POST /api/auth/login
   ```
   - Copy the token from response
   - Set it in Postman environment/collection variable

3. **Create a budget**
   ```
   POST /api/budgets
   ```

4. **Create transactions**
   ```
   POST /api/transactions
   ```

5. **View transactions**
   ```
   GET /api/transactions
   ```

6. **Check budgets (should show spent amount)**
   ```
   GET /api/budgets
   ```

7. **Check alerts (if budget exceeded)**
   ```
   GET /api/alerts
   ```

8. **Update a transaction**
   ```
   PUT /api/transactions/{id}
   ```

9. **Delete a transaction**
   ```
   DELETE /api/transactions/{id}
   ```

---

## Common Issues & Solutions

### 1. PostgreSQL Connection Refused
- **Error**: `Connection to localhost:5432 refused`
- **Cause**: PostgreSQL is not running
- **Solution**: 
  - Check if PostgreSQL service is running
  - On Windows: Open Services and start "postgresql-x64-xx"
  - On Mac: `brew services start postgresql`
  - On Linux: `sudo systemctl start postgresql`
  - Or use Docker: `docker start expense-tracker-db`

### 2. Database Does Not Exist
- **Error**: `database "expense_tracker" does not exist`
- **Solution**:
  ```sql
  CREATE DATABASE expense_tracker;
  ```
  Or let Spring Boot create it by adding to `application.properties`:
  ```properties
  spring.jpa.hibernate.ddl-auto=create
  ```

### 3. JAVA_HOME Not Set (Windows)
- **Error**: `JAVA_HOME not found in your environment`
- **Solution**: 
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Java\jdk-25"
  ```

### 4. 401 Unauthorized
- **Cause**: Missing or invalid JWT token
- **Solution**: 
  - Login again to get a fresh token
  - Ensure Authorization header is set: `Bearer YOUR_TOKEN`
  - Check token hasn't expired (24 hours by default)

### 2. 400 Bad Request
- **Cause**: Invalid request body
- **Solution**: 
  - Verify JSON syntax
  - Check required fields are present
  - Ensure data types match (e.g., amount should be number, not string)

### 3. 404 Not Found
- **Cause**: Invalid endpoint or resource ID
- **Solution**: 
  - Double-check URL spelling
  - Verify the resource ID exists
  - Ensure server is running

### 4. 500 Internal Server Error
- **Cause**: Server-side error
- **Solution**: 
  - Check server logs in terminal
  - Verify database is running
  - Check application.properties configuration

---

## Tips for Effective Testing

1. **Use Environment Variables**
   - Store `baseUrl` and `token` as variables
   - Easy to switch between environments (dev, staging, prod)

2. **Save Responses**
   - Use Postman's "Save Response" feature
   - Compare responses across test runs

3. **Create Test Scripts**
   - Add tests in the "Tests" tab
   - Example: Automatically save token after login
   ```javascript
   pm.test("Login successful", function () {
       pm.response.to.have.status(200);
       var jsonData = pm.response.json();
       pm.collectionVariables.set("token", jsonData.token);
   });
   ```

4. **Use Pre-request Scripts**
   - Generate dynamic data
   - Example: Current date for transactions
   ```javascript
   pm.variables.set("currentDate", new Date().toISOString().split('T')[0]);
   ```

5. **Organize Requests**
   - Group related endpoints in folders
   - Use descriptive names
   - Add documentation in descriptions

---

## Available Categories

When creating or updating transactions, use these categories (case-sensitive):
- `Food` - Groceries, restaurants, dining
- `Transportation` - Gas, parking, public transit, ride-sharing
- `Entertainment` - Movies, games, recreation, events
- `Utilities` - Electric, water, internet, phone bills
- `Healthcare` - Medical, dental, pharmacy, gym
- `Shopping` - Clothing, electronics, general retail
- `Other` - Miscellaneous expenses

**Important**: Categories are case-sensitive. Use exact capitalization (e.g., "Food" not "FOOD" or "food").

## Budget Periods

When creating budgets, you can use:
- `MONTHLY` - Budget for current month (most common)
- `YYYY-MM` - Specific month (e.g., "2026-02" for February 2026)
- `WEEKLY`, `DAILY`, `YEARLY` - Also supported (uses current month)

---

## Next Steps

1. Test all authentication endpoints first
2. Create sample budgets for different categories
3. Add various transactions
4. Test budget tracking and alerts
5. Explore Plaid integration (requires Plaid sandbox credentials)

For more details, refer to the API documentation in the codebase or contact the development team.
