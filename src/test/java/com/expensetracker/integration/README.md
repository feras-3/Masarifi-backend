# End-to-End Integration Tests

This directory contains comprehensive end-to-end integration tests that validate complete user workflows across the Java Spring Boot backend.

## Test Coverage

### EndToEndIntegrationTest.java

Complete backend integration tests covering:

1. **Complete User Flow**: Create transaction, view budget, receive alert
   - Validates Requirements: 2.1, 6.1, 7.1, 7.2, 8.1
   - Tests the full workflow from budget creation through transaction creation and alert generation
   - Verifies budget calculations update correctly
   - Confirms alerts are generated at 80% and 100% thresholds

2. **Transaction Edit and Delete Flows**
   - Validates Requirements: 4.1, 4.2, 6.1
   - Tests editing transaction amounts and categories
   - Tests deleting transactions
   - Verifies budget recalculation after edits and deletions

3. **Budget Creation and Update Flows**
   - Validates Requirements: 1.1, 1.3, 6.3
   - Tests creating initial budgets
   - Tests updating budget amounts
   - Verifies budget status calculations

4. **Multiple Transactions with Category Filtering**
   - Validates Requirements: 2.1, 3.1, 3.2, 5.5
   - Tests creating transactions in multiple categories
   - Verifies transactions are sorted by date descending
   - Tests total amount calculations

5. **Alert Dismissal Flow**
   - Validates Requirements: 7.1, 8.3, 8.4
   - Tests dismissing alerts
   - Verifies dismissed alerts are retained in history

6. **Transaction Flow Without Budget**
   - Validates Requirements: 2.1, 3.1
   - Tests creating transactions when no budget exists
   - Verifies no alerts are generated without a budget

7. **Alert Generation at Exact Thresholds**
   - Validates Requirements: 7.1, 7.2, 7.3
   - Tests alert generation at exactly 80% and 100%
   - Verifies alert idempotency (only one alert per threshold per period)

### PlaidIntegrationFlowTest.java

Plaid integration tests covering:

1. **Complete Plaid Integration Flow**
   - Validates Requirements: 13.1-13.9, 14.1-14.8, 16.1-16.5
   - Tests link token generation
   - Tests public token exchange
   - Tests transaction synchronization
   - Tests viewing imported transactions

2. **Manual Category Changes for Plaid Transactions**
   - Validates Requirements: 15.10, 15.11
   - Tests changing categories on Plaid-imported transactions
   - Verifies original Plaid category is preserved

3. **Account Unlinking with Transaction Retention**
   - Validates Requirements: 17.5, 17.6, 17.7
   - Tests unlinking Plaid accounts
   - Verifies access tokens are deleted
   - Confirms transactions are retained after unlinking

4. **Plaid Transaction Deduplication**
   - Validates Requirements: 14.4, 14.5
   - Tests that duplicate Plaid transactions are not imported
   - Verifies Plaid transaction IDs are unique

5. **Plaid Transactions with Budget Integration**
   - Validates Requirements: 14.1, 6.1, 7.1
   - Tests that Plaid transactions update budget calculations
   - Verifies alerts are generated based on Plaid transactions

6. **Transaction Source Filtering**
   - Validates Requirements: 16.4
   - Tests filtering transactions by source (MANUAL vs PLAID)
   - Verifies Plaid transactions have required fields

7. **Plaid Transaction Display Fields**
   - Validates Requirements: 16.2, 16.3
   - Tests that Plaid transactions include merchant names
   - Verifies original Plaid categories are stored

## Running the Tests

```bash
# Run all integration tests
./mvnw test -Dtest=*IntegrationTest

# Run specific test class
./mvnw test -Dtest=EndToEndIntegrationTest

# Run specific test method
./mvnw test -Dtest=EndToEndIntegrationTest#testCompleteUserFlow_CreateTransactionViewBudgetReceiveAlert

# Run with coverage
./mvnw test jacoco:report
```

## Test Configuration

The tests use:
- **Spring Boot Test** framework with `@SpringBootTest`
- **JUnit 5** for test execution
- **@ActiveProfiles("test")** to use test configuration
- **@Transactional** to rollback database changes after each test
- **H2 in-memory database** for test isolation

## Test Data Setup

Each test:
1. Creates a unique test user ID to avoid conflicts
2. Cleans up all data before running (transactions, budgets, alerts, Plaid accounts)
3. Uses the current period for budget calculations
4. Rolls back all changes after completion

## Plaid Integration Notes

- Tests use mocked Plaid responses since we're in sandbox mode
- PlaidService methods are tested with mock tokens and responses
- Real Plaid API integration would require valid sandbox credentials
- Tests verify the application logic around Plaid integration

## Assertions

Tests verify:
- Correct data persistence and retrieval
- Accurate budget calculations
- Proper alert generation at thresholds
- Transaction sorting and filtering
- Atomic operations (transaction + budget updates)
- Data consistency after operations

## Future Enhancements

- Add tests for concurrent user operations
- Add tests for webhook handling
- Add tests for invalid access token scenarios
- Add tests for Plaid API error handling
- Add performance tests with large datasets
- Add tests for category mapping edge cases
