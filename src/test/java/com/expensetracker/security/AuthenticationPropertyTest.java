package com.expensetracker.security;

import net.jqwik.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for authentication requirements.
 * Feature: expense-tracking-app, Property 35: Authentication Required for Operations
 * 
 * **Validates: Requirements 9.1, 9.4, 9.5**
 * 
 * These tests verify that:
 * - All transaction and budget operations require authentication (Req 9.1)
 * - The API Gateway validates authentication tokens on each request (Req 9.4)
 * - Invalid or missing tokens result in unauthorized errors (Req 9.5)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationPropertyTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Property 35: Authentication Required for Operations
     * 
     * For any transaction or budget operation request without a valid authentication token,
     * the request should be rejected with an unauthorized error.
     * 
     * This test verifies that all protected endpoints return 401 Unauthorized when:
     * 1. No Authorization header is provided
     * 2. An invalid token is provided
     * 3. A malformed token is provided
     */
    @Property(tries = 100)
    @Label("Transaction and budget operations require valid authentication")
    void transactionAndBudgetOperationsRequireAuthentication(
            @ForAll("protectedEndpoints") EndpointConfig endpoint,
            @ForAll("invalidTokens") String invalidToken) throws Exception {
        
        // When: Making a request to a protected endpoint with an invalid or missing token
        var requestBuilder = switch (endpoint.method()) {
            case "GET" -> get(endpoint.path());
            case "POST" -> post(endpoint.path())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(endpoint.sampleBody());
            case "PUT" -> put(endpoint.path())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(endpoint.sampleBody());
            case "DELETE" -> delete(endpoint.path());
            default -> throw new IllegalArgumentException("Unsupported method: " + endpoint.method());
        };

        // Add the invalid token if it's not null (null represents missing token)
        if (invalidToken != null && !invalidToken.isEmpty()) {
            requestBuilder.header("Authorization", invalidToken);
        }

        // Then: The request should be rejected with 401 Unauthorized
        mockMvc.perform(requestBuilder)
                .andExpect(status().isUnauthorized());
    }

    /**
     * Property 35: Authentication Required - No Token Scenario
     * 
     * Verifies that requests without any Authorization header are rejected.
     */
    @Property(tries = 100)
    @Label("Protected endpoints reject requests without authentication token")
    void protectedEndpointsRejectRequestsWithoutToken(
            @ForAll("protectedEndpoints") EndpointConfig endpoint) throws Exception {
        
        // When: Making a request without any Authorization header
        var requestBuilder = switch (endpoint.method()) {
            case "GET" -> get(endpoint.path());
            case "POST" -> post(endpoint.path())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(endpoint.sampleBody());
            case "PUT" -> put(endpoint.path())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(endpoint.sampleBody());
            case "DELETE" -> delete(endpoint.path());
            default -> throw new IllegalArgumentException("Unsupported method: " + endpoint.method());
        };

        // Then: The request should be rejected with 401 Unauthorized
        mockMvc.perform(requestBuilder)
                .andExpect(status().isUnauthorized());
    }

    /**
     * Property 35: Authentication Required - Invalid Token Format
     * 
     * Verifies that requests with malformed Bearer tokens are rejected.
     */
    @Property(tries = 100)
    @Label("Protected endpoints reject requests with malformed tokens")
    void protectedEndpointsRejectMalformedTokens(
            @ForAll("protectedEndpoints") EndpointConfig endpoint,
            @ForAll("malformedBearerTokens") String malformedToken) throws Exception {
        
        // When: Making a request with a malformed Bearer token
        var requestBuilder = switch (endpoint.method()) {
            case "GET" -> get(endpoint.path());
            case "POST" -> post(endpoint.path())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(endpoint.sampleBody());
            case "PUT" -> put(endpoint.path())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(endpoint.sampleBody());
            case "DELETE" -> delete(endpoint.path());
            default -> throw new IllegalArgumentException("Unsupported method: " + endpoint.method());
        };

        requestBuilder.header("Authorization", malformedToken);

        // Then: The request should be rejected with 401 Unauthorized
        mockMvc.perform(requestBuilder)
                .andExpect(status().isUnauthorized());
    }

    // ==================== Arbitraries ====================

    /**
     * Provides a variety of protected endpoints (transaction and budget operations)
     * that should require authentication.
     */
    @Provide
    Arbitrary<EndpointConfig> protectedEndpoints() {
        return Arbitraries.of(
                // Transaction endpoints
                new EndpointConfig("POST", "/api/transactions", 
                        "{\"amount\":100.0,\"date\":\"2024-01-01\",\"description\":\"Test\",\"category\":\"Food\"}"),
                new EndpointConfig("GET", "/api/transactions", ""),
                new EndpointConfig("PUT", "/api/transactions/123", 
                        "{\"amount\":150.0,\"date\":\"2024-01-02\",\"description\":\"Updated\",\"category\":\"Food\"}"),
                new EndpointConfig("DELETE", "/api/transactions/123", ""),
                new EndpointConfig("GET", "/api/transactions/by-category", ""),
                
                // Budget endpoints
                new EndpointConfig("POST", "/api/budgets", 
                        "{\"amount\":1000.0,\"period\":\"2024-01\"}"),
                new EndpointConfig("GET", "/api/budgets/current", ""),
                new EndpointConfig("PUT", "/api/budgets/123", 
                        "{\"amount\":1500.0}"),
                
                // Alert endpoints
                new EndpointConfig("GET", "/api/alerts", ""),
                new EndpointConfig("PUT", "/api/alerts/123/dismiss", "")
        );
    }

    /**
     * Provides various invalid token scenarios:
     * - null (no token)
     * - empty string
     * - random strings without Bearer prefix
     * - expired tokens
     * - tokens with invalid signatures
     */
    @Provide
    Arbitrary<String> invalidTokens() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50),
                Arbitraries.strings().numeric().ofMinLength(20).ofMaxLength(100),
                Arbitraries.just("InvalidToken123"),
                Arbitraries.just("Bearer "),
                Arbitraries.just("Bearer invalid.token.here"),
                Arbitraries.just("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature")
        );
    }

    /**
     * Provides malformed Bearer token formats:
     * - Missing "Bearer " prefix
     * - Extra spaces
     * - Wrong prefix
     * - Invalid JWT structure
     */
    @Provide
    Arbitrary<String> malformedBearerTokens() {
        return Arbitraries.oneOf(
                // Missing Bearer prefix
                Arbitraries.strings().alpha().ofMinLength(20).ofMaxLength(100),
                
                // Wrong prefix
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(10)
                        .map(s -> "Token " + s),
                
                // Extra spaces
                Arbitraries.strings().alpha().ofMinLength(20).ofMaxLength(50)
                        .map(s -> "Bearer  " + s),
                
                // Invalid JWT structure (not three parts separated by dots)
                Arbitraries.strings().alpha().ofMinLength(20).ofMaxLength(50)
                        .map(s -> "Bearer " + s),
                
                // JWT with only two parts
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(20)
                        .map(s -> "Bearer " + s + "." + s),
                
                // Empty Bearer token
                Arbitraries.just("Bearer ")
        );
    }

    /**
     * Record to represent an endpoint configuration for testing.
     */
    record EndpointConfig(String method, String path, String sampleBody) {}
}
