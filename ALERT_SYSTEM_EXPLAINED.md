# Alert System - How It Works

## Overview

The AlertService automatically monitors your budget spending and creates alerts when you reach certain thresholds. It's designed to help users stay aware of their spending habits and avoid budget overruns.

## How Alerts Are Triggered

### Automatic Trigger Points

Alerts are automatically checked and created whenever:
1. **A transaction is created** - After saving a new transaction
2. **A transaction is updated** - After modifying an existing transaction
3. **A transaction is deleted** - After removing a transaction
4. **Budget is recalculated** - When the system recalculates budget balances

### Alert Thresholds

The system monitors two critical thresholds:

1. **WARNING Alert** - Triggered at **80%** of budget
   - Type: `WARNING`
   - Message: You've used 80% or more of your budget
   - Purpose: Early warning to slow down spending

2. **CRITICAL Alert** - Triggered at **100%** of budget
   - Type: `CRITICAL`
   - Message: You've reached or exceeded your budget
   - Purpose: Urgent notification that budget is exhausted

## Alert Lifecycle

### 1. Alert Creation

When a transaction causes spending to cross a threshold:

```
Transaction Created → Budget Recalculated → Check Thresholds → Create Alert (if needed)
```

**Example Flow:**
- Budget: $500 for February 2026
- Current spending: $350 (70%)
- New transaction: $100
- New total: $450 (90%)
- **Result**: WARNING alert created (crossed 80% threshold)

### 2. Alert Deduplication

The system ensures **only one alert per threshold per period**:

- Database constraint: `UNIQUE(user_id, type, period)`
- Before creating an alert, checks if one already exists
- If alert exists, no duplicate is created
- This prevents spam when multiple transactions push spending over the threshold

**Example:**
- First transaction crosses 80% → WARNING alert created
- Second transaction still at 85% → No new alert (WARNING already exists)
- Third transaction crosses 100% → CRITICAL alert created (different type)

### 3. Alert Retrieval

Users can retrieve alerts through the API:

**GET /api/alerts**
- Returns all alerts (dismissed and active)
- Includes unread count
- Sorted by creation date (newest first)

**Response:**
```json
{
  "alerts": [
    {
      "id": "uuid",
      "userId": "testuser",
      "type": "CRITICAL",
      "budgetAmount": 500.00,
      "currentSpending": 520.00,
      "percentageExceeded": 104.00,
      "period": "2026-02",
      "dismissed": false,
      "createdAt": "2026-02-26T15:30:00"
    },
    {
      "id": "uuid",
      "userId": "testuser",
      "type": "WARNING",
      "budgetAmount": 500.00,
      "currentSpending": 410.00,
      "percentageExceeded": 82.00,
      "period": "2026-02",
      "dismissed": false,
      "createdAt": "2026-02-26T10:00:00"
    }
  ],
  "unreadCount": 2
}
```

### 4. Alert Dismissal

Users can dismiss alerts they've acknowledged:

**PUT /api/alerts/{id}/dismiss**
- Marks the alert as `dismissed: true`
- Alert remains in database but marked as read
- Reduces unread count
- Does not delete the alert (keeps history)

## Alert Properties

Each alert contains:

| Property | Description | Example |
|----------|-------------|---------|
| `id` | Unique identifier | "uuid-123" |
| `userId` | Owner of the alert | "testuser" |
| `type` | Alert severity | "WARNING" or "CRITICAL" |
| `budgetAmount` | Budget limit | 500.00 |
| `currentSpending` | Spending when alert created | 410.00 |
| `percentageExceeded` | Percentage of budget used | 82.00 |
| `period` | Budget period | "2026-02" |
| `dismissed` | Whether user acknowledged | false |
| `createdAt` | When alert was created | "2026-02-26T10:00:00" |

## Code Flow

### When a Transaction is Created:

```java
// 1. Transaction is saved
Transaction saved = transactionRepository.save(transaction);

// 2. Budget recalculation is triggered
budgetService.recalculateBalance(userId);

// 3. Inside recalculateBalance:
Budget budget = getCurrentBudget(userId);
BigDecimal spent = transactionRepository.getTotalSpending(userId, startDate, endDate);

// 4. Check thresholds
alertService.checkBudgetThresholds(userId, spent, budget.getAmount(), budget.getPeriod());

// 5. Inside checkBudgetThresholds:
BigDecimal percentageUsed = (spent / budgetAmount) * 100;

if (percentageUsed >= 100) {
    generateAlertIfNotExists(userId, "CRITICAL", ...);
}
if (percentageUsed >= 80) {
    generateAlertIfNotExists(userId, "WARNING", ...);
}

// 6. Alert is created only if it doesn't exist
Optional<Alert> existing = alertRepository.findByUserIdAndTypeAndPeriod(...);
if (existing.isEmpty()) {
    Alert alert = new Alert(...);
    alertRepository.save(alert);
}
```

## Important Notes

### Alert Persistence
- Alerts are **NOT deleted** when dismissed
- They remain in the database with `dismissed: true`
- This maintains a history of budget warnings
- Useful for analytics and spending pattern analysis

### Period-Based Alerts
- Alerts are tied to specific budget periods (e.g., "2026-02")
- Each period can have its own WARNING and CRITICAL alerts
- When a new period starts, new alerts can be created
- Old period alerts remain in history

### Transaction Propagation
- Alert creation uses `REQUIRES_NEW` transaction propagation
- If alert creation fails, it won't rollback the transaction
- This ensures transactions are saved even if alerting fails
- Errors are logged but don't block the main operation

## Testing Alerts

### Scenario 1: Trigger WARNING Alert

1. Create a budget:
   ```json
   POST /api/budgets
   {
     "amount": 100.00,
     "period": "MONTHLY"
   }
   ```

2. Create transactions totaling $80+:
   ```json
   POST /api/transactions
   {
     "amount": 85.00,
     "category": "Food",
     "description": "Groceries",
     "date": "2026-02-26"
   }
   ```

3. Check alerts:
   ```
   GET /api/alerts
   ```
   Should see WARNING alert with 85% usage

### Scenario 2: Trigger CRITICAL Alert

1. Add more transactions to exceed budget:
   ```json
   POST /api/transactions
   {
     "amount": 20.00,
     "category": "Food",
     "description": "Lunch",
     "date": "2026-02-26"
   }
   ```

2. Check alerts:
   ```
   GET /api/alerts
   ```
   Should see both WARNING (85%) and CRITICAL (105%) alerts

### Scenario 3: Dismiss Alert

1. Get alert ID from previous response
2. Dismiss it:
   ```
   PUT /api/alerts/{id}/dismiss
   ```
3. Check alerts again - unreadCount should decrease

## API Endpoints

### Get All Alerts
```
GET /api/alerts
Authorization: Bearer {token}
```

### Dismiss Alert
```
PUT /api/alerts/{id}/dismiss
Authorization: Bearer {token}
```

## Database Schema

```sql
CREATE TABLE alerts (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    budget_amount DECIMAL(10,2) NOT NULL,
    current_spending DECIMAL(10,2) NOT NULL,
    percentage_exceeded DECIMAL(5,2) NOT NULL,
    period VARCHAR(20) NOT NULL,
    dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_type_period UNIQUE (user_id, type, period)
);
```

The unique constraint ensures no duplicate alerts for the same user, type, and period.

## Future Enhancements

Possible improvements to the alert system:

1. **Email/SMS Notifications** - Send alerts via email or SMS
2. **Custom Thresholds** - Let users set their own warning percentages
3. **Category-Specific Alerts** - Alerts for individual spending categories
4. **Alert Preferences** - Users can enable/disable certain alert types
5. **Alert History Dashboard** - Visualize alert patterns over time
6. **Predictive Alerts** - Warn users before they hit thresholds based on spending trends
