# Category-Based Budget Tracking

## Overview

The budget system now supports category-specific budgets, allowing users to create separate budgets for different spending categories (e.g., Food, Transportation, Entertainment).

## Key Changes

### 1. Budget Model

- Added `category` field (optional)
- Updated unique constraint to `(user_id, period, category)` to allow multiple budgets per period

### 2. Database Schema

- Updated `budgets` table with `category` column
- Changed unique constraint from `(user_id, period)` to `(user_id, period, category)`

### 3. Budget Tracking Logic

- **Category-specific spending**: When a budget has a category, only transactions matching that category count toward the budget
- **General budget**: Budgets without a category track all spending (backward compatible)

### 4. Transaction Integration

- Manual transactions automatically update relevant category budgets
- Plaid-synced transactions also update category budgets based on mapped categories

### 5. Repository Updates

- Added `getTotalSpendingByCategory()` to calculate spending for specific categories
- Added `findByUserIdAndPeriodAndCategory()` to find category-specific budgets

## Usage Examples

### Create a category-specific budget

```json
POST /api/budgets
{
  "amount": 500.00,
  "period": "2026-03",
  "category": "Food"
}
```

### Create a general budget (all categories)

```json
POST /api/budgets
{
  "amount": 2000.00,
  "period": "2026-03"
}
```

### Create a transaction

When a transaction is created with a category, it will:

1. Update the category-specific budget (if one exists)
2. Update the general budget (if one exists)
3. Trigger alerts if thresholds are exceeded

```json
POST /api/transactions
{
  "amount": 50.00,
  "date": "2026-03-08",
  "description": "Grocery shopping",
  "category": "Food"
}
```

## Plaid Integration

When transactions are synced from Plaid:

1. Plaid categories are mapped to application categories using `CategoryMapper`
2. Each transaction updates the relevant category budget
3. Budget recalculation runs for all user budgets after sync

## Backward Compatibility

- Existing budgets without categories continue to work
- General budgets (no category) track all spending
- Users can have both general and category-specific budgets simultaneously
