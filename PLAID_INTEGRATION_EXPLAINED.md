# Plaid Integration - How It Works

## Overview

Plaid is a financial technology service that allows applications to securely connect to users' bank accounts and retrieve transaction data. This integration enables automatic transaction importing from linked bank accounts.

## What is Plaid?

**Plaid** acts as a bridge between your application and financial institutions:
- Users authenticate with their bank through Plaid's secure interface
- Plaid retrieves transaction data from the bank
- Your application receives the transaction data through Plaid's API
- All sensitive banking credentials are handled by Plaid, not your app

## Integration Flow

### 1. Link Token Creation (Step 1)

**Endpoint**: `POST /api/plaid/link-token`

**Purpose**: Generate a temporary token to initialize Plaid Link (the UI component)

**Flow**:
```
Frontend → Backend → Plaid API → Backend → Frontend
```

**What Happens**:
1. Frontend requests a link token
2. Backend calls Plaid API with user ID and configuration
3. Plaid generates a temporary link token (expires in 30 minutes)
4. Backend returns link token to frontend
5. Frontend uses this token to initialize Plaid Link UI

**Request**:
```
POST /api/plaid/link-token
Authorization: Bearer {jwt_token}
```

**Response**:
```json
{
  "linkToken": "link-sandbox-xxxxx-xxxxx"
}
```

**Code Flow**:
```java
// Backend creates request to Plaid
LinkTokenCreateRequest request = new LinkTokenCreateRequest()
    .user(new LinkTokenCreateRequestUser().clientUserId(userId))
    .clientName("Expense Tracker")
    .products(Arrays.asList(Products.TRANSACTIONS))
    .countryCodes(Arrays.asList(CountryCode.US))
    .language("en");

// Call Plaid API
Response<LinkTokenCreateResponse> response = plaidClient
    .linkTokenCreate(request)
    .execute();

// Return link token
return response.body().getLinkToken();
```

---

### 2. User Authentication (Step 2)

**This happens in the frontend using Plaid Link UI**

**Flow**:
```
User → Plaid Link UI → Bank → Plaid → Frontend
```

**What Happens**:
1. Frontend initializes Plaid Link with the link token
2. User sees Plaid's secure UI modal
3. User selects their bank
4. User enters bank credentials (handled by Plaid, not your app)
5. Bank authenticates user
6. Plaid generates a **public token**
7. Frontend receives the public token

**Important**: The public token is temporary and must be exchanged for an access token within 30 minutes.

---

### 3. Token Exchange (Step 3)

**Endpoint**: `POST /api/plaid/exchange-token`

**Purpose**: Exchange the temporary public token for a permanent access token

**Flow**:
```
Frontend → Backend → Plaid API → Backend → Database
```

**What Happens**:
1. Frontend sends public token to backend
2. Backend exchanges public token for access token with Plaid
3. Backend retrieves institution information (bank name)
4. Backend **encrypts** the access token (security!)
5. Backend stores encrypted token in database
6. Backend returns success with account info

**Request**:
```json
POST /api/plaid/exchange-token
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "publicToken": "public-sandbox-xxxxx"
}
```

**Response**:
```json
{
  "success": true,
  "accountId": "uuid",
  "institutionName": "Chase"
}
```

**Code Flow**:
```java
// Exchange public token for access token
ItemPublicTokenExchangeRequest request = new ItemPublicTokenExchangeRequest()
    .publicToken(publicToken);

Response<ItemPublicTokenExchangeResponse> response = plaidClient
    .itemPublicTokenExchange(request)
    .execute();

String accessToken = response.body().getAccessToken();
String itemId = response.body().getItemId();

// Get institution name
String institutionName = getInstitutionName(accessToken);

// Encrypt access token before storing (IMPORTANT!)
String encryptedToken = encryptionService.encrypt(accessToken);

// Save to database
PlaidAccount plaidAccount = new PlaidAccount(
    userId, 
    encryptedToken, 
    itemId, 
    institutionName
);
plaidAccountRepository.save(plaidAccount);
```

**Security Note**: Access tokens are encrypted before storage using AES encryption. This ensures that even if the database is compromised, the tokens cannot be used.

---

### 4. Transaction Sync (Step 4)

**Endpoint**: `POST /api/plaid/sync-transactions`

**Purpose**: Fetch transactions from linked bank accounts

**Flow**:
```
Frontend → Backend → Decrypt Token → Plaid API → Map Categories → Save Transactions → Trigger Alerts
```

**What Happens**:
1. Backend retrieves user's active Plaid accounts
2. For each account:
   - Decrypt the access token
   - Request transactions from past 30 days
   - Filter out pending transactions
   - Check for duplicates (using Plaid transaction ID)
   - Map Plaid categories to app categories
   - Create Transaction records with source="PLAID"
   - Save to database
3. Trigger budget recalculation and alert checking
4. Return sync result

**Request**:
```
POST /api/plaid/sync-transactions
Authorization: Bearer {jwt_token}
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

**Code Flow**:
```java
// Get active Plaid accounts
List<PlaidAccount> accounts = plaidAccountRepository
    .findByUserIdAndIsActive(userId, true);

for (PlaidAccount account : accounts) {
    // Decrypt access token
    String accessToken = encryptionService.decrypt(account.getAccessToken());
    
    // Request transactions from Plaid
    TransactionsGetRequest request = new TransactionsGetRequest()
        .accessToken(accessToken)
        .startDate(LocalDate.now().minusDays(30))
        .endDate(LocalDate.now());
    
    Response<TransactionsGetResponse> response = plaidClient
        .transactionsGet(request)
        .execute();
    
    // Process each transaction
    for (Transaction plaidTx : response.body().getTransactions()) {
        // Skip if pending or already exists
        if (plaidTx.getPending() || alreadyExists(plaidTx.getId())) {
            continue;
        }
        
        // Map Plaid category to app category
        String appCategory = categoryMapper.mapPlaidCategory(
            plaidTx.getPersonalFinanceCategory().getPrimary()
        );
        
        // Create and save transaction
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(Math.abs(plaidTx.getAmount()));
        transaction.setCategory(appCategory);
        transaction.setPlaidTransactionId(plaidTx.getTransactionId());
        transaction.setSource("PLAID");
        // ... set other fields
        
        transactionRepository.save(transaction);
    }
}

// Trigger budget recalculation
budgetService.recalculateBalance(userId);
```

---

### 5. Category Mapping

**Purpose**: Convert Plaid's categories to your app's categories

Plaid uses detailed categories like:
- "Food and Drink" → Maps to "Food"
- "Transportation" → Maps to "Transportation"
- "Restaurants" → Maps to "Food"
- "Gas" → Maps to "Transportation"

**Mapping Logic**:
```java
public String mapPlaidCategory(String plaidCategory) {
    // Exact match
    if (categoryMappings.containsKey(plaidCategory)) {
        return categoryMappings.get(plaidCategory);
    }
    
    // Partial match (case-insensitive)
    for (Entry<String, String> entry : categoryMappings.entrySet()) {
        if (plaidCategory.toLowerCase().contains(entry.getKey().toLowerCase())) {
            return entry.getValue();
        }
    }
    
    // Default to "Other"
    return "Other";
}
```

---

### 6. Get Linked Accounts

**Endpoint**: `GET /api/plaid/accounts`

**Purpose**: Check which bank accounts are linked

**Request**:
```
GET /api/plaid/accounts
Authorization: Bearer {jwt_token}
```

**Response**:
```json
{
  "accounts": [
    {
      "id": "uuid",
      "institutionName": "Chase",
      "itemId": "item-xxx",
      "isActive": true,
      "linkedAt": "2026-02-26T10:00:00",
      "lastSyncAt": "2026-02-26T15:30:00"
    }
  ],
  "linked": true
}
```

---

### 7. Unlink Account

**Endpoint**: `DELETE /api/plaid/unlink`

**Purpose**: Disconnect bank account (marks as inactive, keeps transaction history)

**Request**:
```
DELETE /api/plaid/unlink
Authorization: Bearer {jwt_token}
```

**Response**:
```json
{
  "success": true
}
```

**What Happens**:
- Sets `isActive = false` on PlaidAccount
- Does NOT delete the account record
- Does NOT delete imported transactions
- Prevents future syncs from this account

---

### 8. Webhook Handling (Optional)

**Endpoint**: `POST /api/plaid/webhook`

**Purpose**: Receive notifications from Plaid when new transactions are available

**Flow**:
```
Bank → Plaid → Your Backend → Auto Sync
```

**What Happens**:
1. Bank posts new transactions to Plaid
2. Plaid sends webhook to your backend
3. Backend identifies the account by item ID
4. Backend automatically triggers transaction sync
5. New transactions are imported without user action

**Webhook Types**:
- `TRANSACTIONS.DEFAULT_UPDATE` - New transactions available
- `TRANSACTIONS.INITIAL_UPDATE` - First batch of transactions ready
- `ITEM.ERROR` - Access token expired or invalid

**Request from Plaid**:
```json
{
  "webhookType": "TRANSACTIONS",
  "webhookCode": "DEFAULT_UPDATE",
  "itemId": "item-xxx",
  "newTransactions": 5
}
```

**Code Flow**:
```java
public void handleWebhook(WebhookRequest webhookRequest) {
    if ("TRANSACTIONS".equals(webhookRequest.getWebhookType())) {
        // Find account by item ID
        PlaidAccount account = findByItemId(webhookRequest.getItemId());
        
        // Trigger automatic sync
        syncTransactions(account.getUserId());
    }
}
```

---

## Complete User Flow Example

### Scenario: User wants to link their Chase bank account

**Step 1: Initialize Plaid Link**
```javascript
// Frontend
const response = await fetch('/api/plaid/link-token', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const { linkToken } = await response.json();
```

**Step 2: Open Plaid Link UI**
```javascript
// Frontend
const plaid = Plaid.create({
  token: linkToken,
  onSuccess: (publicToken) => {
    // User successfully authenticated with bank
    exchangeToken(publicToken);
  }
});
plaid.open();
```

**Step 3: User Authenticates**
- User sees Plaid modal
- Selects "Chase"
- Enters Chase username/password
- Completes 2FA if required
- Plaid returns public token

**Step 4: Exchange Token**
```javascript
// Frontend
await fetch('/api/plaid/exchange-token', {
  method: 'POST',
  headers: { 
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ publicToken })
});
```

**Step 5: Sync Transactions**
```javascript
// Frontend
const result = await fetch('/api/plaid/sync-transactions', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` }
});
const { newTransactionCount } = await result.json();
console.log(`Imported ${newTransactionCount} transactions`);
```

**Step 6: View Transactions**
```javascript
// Frontend
const transactions = await fetch('/api/transactions', {
  headers: { 'Authorization': `Bearer ${token}` }
});
// Transactions now include both manual and Plaid-imported ones
```

---

## Transaction Properties

Plaid-imported transactions have additional fields:

| Field | Description | Example |
|-------|-------------|---------|
| `source` | Where transaction came from | "PLAID" |
| `plaidTransactionId` | Unique ID from Plaid | "tx_abc123" |
| `merchantName` | Merchant name from Plaid | "Starbucks" |
| `plaidCategory` | Original Plaid category | "Food and Drink" |
| `category` | Mapped app category | "Food" |

**Example Transaction**:
```json
{
  "id": "uuid",
  "userId": "testuser",
  "amount": 4.50,
  "date": "2026-02-25",
  "description": "Starbucks",
  "category": "Food",
  "source": "PLAID",
  "plaidTransactionId": "tx_abc123",
  "merchantName": "Starbucks",
  "plaidCategory": "Coffee Shop"
}
```

---

## Security Features

### 1. Token Encryption
- Access tokens are encrypted using AES-256
- Encryption key stored in environment variable
- Tokens are decrypted only when needed for API calls

### 2. No Credential Storage
- User bank credentials never touch your servers
- All authentication handled by Plaid
- Your app only stores encrypted access tokens

### 3. Token Validation
- Tokens can be validated before use
- Invalid tokens are detected and handled
- Users can re-authenticate if token expires

### 4. Secure Communication
- All API calls use HTTPS
- Plaid uses OAuth 2.0 standards
- Webhook signatures can be verified (optional)

---

## Configuration

### Environment Variables

```properties
# Plaid Configuration
plaid.client.id=your-plaid-client-id
plaid.secret=your-plaid-sandbox-secret
plaid.environment=sandbox

# Encryption Configuration
encryption.secret=your-encryption-key-32-chars-min
```

### Environments

- **Sandbox**: Testing with fake banks and data
- **Development**: Testing with real banks, limited features
- **Production**: Live environment with real data

---

## Error Handling

### Common Errors

**1. Invalid Public Token**
```json
{
  "error": "Validation failed",
  "message": "Failed to exchange public token: Bad Request"
}
```
**Solution**: Token expired (30 min limit), user needs to re-authenticate

**2. No Active Accounts**
```json
{
  "success": false,
  "errors": ["No active Plaid accounts found"]
}
```
**Solution**: User needs to link a bank account first

**3. Access Token Expired**
```json
{
  "error": "Failed to fetch transactions: Unauthorized"
}
```
**Solution**: User needs to re-link their account

---

## Testing with Postman

### 1. Create Link Token
```
POST http://localhost:8080/api/plaid/link-token
Authorization: Bearer {your_jwt}
```

### 2. Use Plaid Link (Frontend Only)
This step requires the frontend UI - cannot be done in Postman

### 3. Exchange Token (Use test token)
```
POST http://localhost:8080/api/plaid/exchange-token
Authorization: Bearer {your_jwt}
Content-Type: application/json

{
  "publicToken": "public-sandbox-test-token"
}
```

### 4. Sync Transactions
```
POST http://localhost:8080/api/plaid/sync-transactions
Authorization: Bearer {your_jwt}
```

### 5. Check Accounts
```
GET http://localhost:8080/api/plaid/accounts
Authorization: Bearer {your_jwt}
```

---

## Limitations & Considerations

### Transaction History
- Plaid typically provides 2 years of history
- Initial sync may take time for large histories
- Sync fetches last 30 days by default

### Sync Frequency
- Manual sync via button click
- Automatic sync via webhooks (if configured)
- Recommended: Daily automatic syncs

### Duplicate Prevention
- Uses `plaidTransactionId` to prevent duplicates
- Same transaction won't be imported twice
- Updates to transactions are not currently handled

### Category Accuracy
- Plaid's categorization is ~90% accurate
- Users can manually change categories
- Category changes don't affect Plaid data

---

## Future Enhancements

Possible improvements:

1. **Balance Tracking** - Show current account balances
2. **Multiple Accounts** - Link multiple banks per user
3. **Transaction Updates** - Handle transaction modifications
4. **Recurring Detection** - Identify recurring transactions
5. **Spending Insights** - AI-powered spending analysis
6. **Account Refresh** - Re-authenticate expired tokens
7. **Webhook Verification** - Verify webhook signatures
8. **Transaction Filtering** - Filter by account or date range

---

## Resources

- [Plaid Documentation](https://plaid.com/docs/)
- [Plaid API Reference](https://plaid.com/docs/api/)
- [Plaid Link Guide](https://plaid.com/docs/link/)
- [Plaid Sandbox Testing](https://plaid.com/docs/sandbox/)
