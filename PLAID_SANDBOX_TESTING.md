# Plaid Sandbox Testing with Postman

## Overview

This guide shows you how to test Plaid integration using Postman with the Sandbox environment. The Sandbox flow is simpler than production and doesn't require the Plaid Link UI.

## Prerequisites

1. Plaid Sandbox credentials in `application.properties`:
```properties
plaid.client.id=your-plaid-client-id
plaid.secret=your-plaid-sandbox-secret
plaid.environment=sandbox
```

2. Backend server running
3. Valid JWT token from login

## Sandbox Flow (3 Steps)

### Step 1: Create Sandbox Public Token

**Endpoint**: `POST /api/plaid/public-token`

This endpoint creates a public token directly without requiring Plaid Link UI. It uses the Plaid credentials configured in your `application.properties` file.

**Request**:
```http
POST http://localhost:8080/api/plaid/public-token
Content-Type: application/json

{
  "institutionId": "ins_109508"
}
```

**Request Parameters**:
- `institutionId` (required): The sandbox institution ID to link

**Common Institution IDs** (Sandbox):
- `ins_109508` - First Platypus Bank (default)
- `ins_109509` - First Gingham Credit Union
- `ins_109510` - Tattersall Federal Credit Union
- `ins_109511` - Tartan Bank

**Response**:
```json
{
  "publicToken": "public-sandbox-xxxxx-xxxxx"
}
```

**What Happens**:
- Endpoint uses Plaid credentials from `application.properties` (plaid.client.id and plaid.secret)
- Calls Plaid's sandbox API to create a public token for the specified institution
- Returns a public token that represents a fake bank account with test data
- No user authentication required (it's fake data)

---

### Step 2: Exchange Public Token for Access Token

**Endpoint**: `POST /api/plaid/exchange-token`

Exchange the public token for a permanent access token.

**Request**:
```http
POST http://localhost:8080/api/plaid/exchange-token
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "publicToken": "public-sandbox-xxxxx-xxxxx"
}
```

**Response**:
```json
{
  "success": true,
  "accountId": "uuid-generated",
  "institutionName": "First Platypus Bank"
}
```

**What Happens**:
- Backend exchanges public token with Plaid for access token
- Access token is encrypted and stored in database
- Institution information is retrieved and stored
- Account is marked as active

---

### Step 3: Sync Transactions

**Endpoint**: `POST /api/plaid/sync-transactions`

Fetch transactions from the linked sandbox account.

**Request**:
```http
POST http://localhost:8080/api/plaid/sync-transactions
Authorization: Bearer YOUR_JWT_TOKEN
```

**Response**:
```json
{
  "success": true,
  "added": 15,
  "modified": 0,
  "removed": 0,
  "newTransactionCount": 15
}
```

**What Happens**:
- Backend retrieves transactions from Plaid (fake sandbox data)
- Transactions are mapped to app categories
- Transactions are saved with `source="PLAID"`
- Budget is recalculated and alerts are checked

---

### Step 4: View Imported Transactions

**Endpoint**: `GET /api/transactions`

View all transactions including Plaid-imported ones.

**Request**:
```http
GET http://localhost:8080/api/transactions
Authorization: Bearer YOUR_JWT_TOKEN
```

**Response**:
```json
[
  {
    "id": "uuid",
    "userId": "testuser",
    "amount": 12.50,
    "date": "2026-02-20",
    "description": "Uber",
    "category": "Transportation",
    "source": "PLAID",
    "plaidTransactionId": "tx_sandbox_123",
    "merchantName": "Uber",
    "plaidCategory": "Transportation",
    "createdAt": "2026-02-26T10:00:00"
  },
  {
    "id": "uuid",
    "userId": "testuser",
    "amount": 45.00,
    "date": "2026-02-21",
    "description": "Whole Foods",
    "category": "Food",
    "source": "PLAID",
    "plaidTransactionId": "tx_sandbox_124",
    "merchantName": "Whole Foods",
    "plaidCategory": "Groceries",
    "createdAt": "2026-02-26T10:00:00"
  }
]
```

---

## Complete Postman Test Flow

### 1. Login
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```
Save the token from response.

### 2. Create Budget (Optional but recommended)
```http
POST http://localhost:8080/api/budgets
Authorization: Bearer YOUR_TOKEN
Content-Type: application/json

{
  "amount": 1000.00,
  "period": "MONTHLY"
}
```

### 3. Create Sandbox Public Token
```http
POST http://localhost:8080/api/plaid/public-token
Content-Type: application/json

{
  "institutionId": "ins_109508"
}
```
Copy the `publicToken` from response.

### 4. Exchange Token
```http
POST http://localhost:8080/api/plaid/exchange-token
Authorization: Bearer YOUR_TOKEN
Content-Type: application/json

{
  "publicToken": "PASTE_PUBLIC_TOKEN_HERE"
}
```

### 5. Sync Transactions
```http
POST http://localhost:8080/api/plaid/sync-transactions
Authorization: Bearer YOUR_TOKEN
```

### 6. View Transactions
```http
GET http://localhost:8080/api/transactions
Authorization: Bearer YOUR_TOKEN
```

### 7. Check Budget Status
```http
GET http://localhost:8080/api/budgets/current
Authorization: Bearer YOUR_TOKEN
```

### 8. Check Alerts
```http
GET http://localhost:8080/api/alerts
Authorization: Bearer YOUR_TOKEN
```

---

## Sandbox Test Data

Plaid's Sandbox provides fake but realistic transaction data:

**Transaction Types**:
- Groceries (Whole Foods, Trader Joe's)
- Restaurants (McDonald's, Starbucks)
- Transportation (Uber, Lyft)
- Shopping (Amazon, Target)
- Utilities (Electric, Internet)
- Entertainment (Netflix, Spotify)

**Transaction Amounts**: Range from $5 to $500
**Transaction Dates**: Last 30 days
**Transaction Count**: Typically 10-20 transactions

---

## Sandbox Institutions

You can test with different fake banks:

| Institution ID | Name | Description |
|---------------|------|-------------|
| `ins_109508` | First Platypus Bank | Default test bank |
| `ins_109509` | First Gingham Credit Union | Alternative test bank |
| `ins_109510` | Tattersall Federal Credit Union | Another test option |
| `ins_109511` | Tartan Bank | Classic test bank |

Each institution provides similar test data but with different institution names.

---

## Testing Scenarios

### Scenario 1: Basic Import
1. Create public token
2. Exchange for access token
3. Sync transactions
4. Verify transactions appear in GET /api/transactions

### Scenario 2: Budget Integration
1. Create a budget for $500
2. Link Plaid account
3. Sync transactions (total > $400)
4. Check alerts - should see WARNING alert

### Scenario 3: Multiple Syncs
1. Link account and sync
2. Wait a moment
3. Sync again
4. Verify no duplicate transactions (same plaidTransactionId)

### Scenario 4: Unlink and Relink
1. Link account and sync
2. Unlink account (DELETE /api/plaid/unlink)
3. Verify transactions are still there
4. Link again with new public token
5. Sync - should not create duplicates

---

## Common Issues

### Issue 1: "Failed to create sandbox public token"
**Cause**: Invalid institution ID or Plaid credentials
**Solution**: 
- Check `plaid.client.id` and `plaid.secret` in application.properties
- Verify using a valid sandbox institution ID
- Ensure `plaid.environment=sandbox`

### Issue 2: "No active Plaid accounts found"
**Cause**: Token exchange failed or account not active
**Solution**:
- Check if exchange-token succeeded
- Verify account in database: `SELECT * FROM plaid_accounts WHERE user_id='testuser'`
- Ensure `is_active=true`

### Issue 3: Duplicate transactions on multiple syncs
**Cause**: Should not happen - plaidTransactionId prevents duplicates
**Solution**:
- Check database for duplicate plaidTransactionId
- Verify deduplication logic in PlaidService

### Issue 4: Transactions not appearing
**Cause**: Sync succeeded but transactions not saved
**Solution**:
- Check server logs for errors
- Verify category mapping is working
- Check if transactions are pending (pending transactions are skipped)

---

## Differences: Sandbox vs Production

| Feature | Sandbox | Production |
|---------|---------|------------|
| Authentication | No real credentials | Real bank login required |
| Data | Fake test data | Real transaction data |
| Public Token | Created via API | Created via Plaid Link UI |
| Institution IDs | Test IDs (ins_109xxx) | Real IDs |
| Transaction History | Last 30 days fake data | Up to 2 years real data |
| Updates | Static data | Real-time updates |
| Webhooks | Can be simulated | Real notifications |

---

## Production Flow (For Reference)

In production, the flow is different:

1. **Backend**: Create link token (`POST /api/plaid/link-token`)
2. **Frontend**: Initialize Plaid Link UI with link token
3. **User**: Authenticate with real bank through Plaid UI
4. **Frontend**: Receive public token from Plaid Link
5. **Frontend**: Send public token to backend (`POST /api/plaid/exchange-token`)
6. **Backend**: Exchange for access token and store
7. **Backend**: Sync transactions (`POST /api/plaid/sync-transactions`)

The sandbox endpoint (`/sandbox/public-token`) bypasses steps 2-4 for testing purposes.

---

## Security Notes

### Sandbox
- Sandbox credentials can be shared for testing
- No real financial data is accessed
- Safe to commit sandbox credentials to version control (though not recommended)

### Production
- NEVER commit production credentials
- Use environment variables
- Rotate secrets regularly
- Enable webhook signature verification
- Use HTTPS only

---

## Next Steps

After testing in Sandbox:

1. **Get Production Credentials**: Apply for Plaid production access
2. **Update Configuration**: Change `plaid.environment=production`
3. **Implement Plaid Link**: Add frontend UI for real authentication
4. **Remove Sandbox Endpoint**: Disable `/sandbox/public-token` in production
5. **Enable Webhooks**: Set up webhook URL for automatic syncs
6. **Add Error Handling**: Handle token expiration and re-authentication

---

## Useful Plaid Sandbox Endpoints

For direct testing (outside your app):

**Create Public Token**:
```bash
curl -X POST https://sandbox.plaid.com/sandbox/public_token/create \
  -H 'Content-Type: application/json' \
  -d '{
    "client_id": "YOUR_CLIENT_ID",
    "secret": "YOUR_SECRET",
    "institution_id": "ins_109508",
    "initial_products": ["transactions"]
  }'
```

**Exchange Token**:
```bash
curl -X POST https://sandbox.plaid.com/item/public_token/exchange \
  -H 'Content-Type: application/json' \
  -d '{
    "client_id": "YOUR_CLIENT_ID",
    "secret": "YOUR_SECRET",
    "public_token": "public-sandbox-xxxxx"
  }'
```

**Get Transactions**:
```bash
curl -X POST https://sandbox.plaid.com/transactions/get \
  -H 'Content-Type: application/json' \
  -d '{
    "client_id": "YOUR_CLIENT_ID",
    "secret": "YOUR_SECRET",
    "access_token": "access-sandbox-xxxxx",
    "start_date": "2026-01-01",
    "end_date": "2026-02-26"
  }'
```

---

## Resources

- [Plaid Sandbox Guide](https://plaid.com/docs/sandbox/)
- [Plaid API Reference](https://plaid.com/docs/api/)
- [Sandbox Test Credentials](https://plaid.com/docs/sandbox/test-credentials/)
- [Institution IDs](https://plaid.com/docs/sandbox/institutions/)
