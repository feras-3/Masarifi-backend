package com.expensetracker.config;

import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class PlaidConfig {
    
    @Value("${plaid.client.id}")
    private String clientId;
    
    @Value("${plaid.secret}")
    private String secret;
    
    @Value("${plaid.environment:sandbox}")
    private String environment;
    
    @Bean
    public PlaidApi plaidClient() {
        // Set up API keys
        HashMap<String, String> apiKeys = new HashMap<>();
        apiKeys.put("clientId", clientId);
        apiKeys.put("secret", secret);
        
        // Create ApiClient
        ApiClient apiClient = new ApiClient(apiKeys);
        
        // Set the appropriate environment adapter
        switch (environment.toLowerCase()) {
            case "production":
                apiClient.setPlaidAdapter(ApiClient.Production);
                break;
            case "development":
                apiClient.setPlaidAdapter(ApiClient.Development);
                break;
            case "sandbox":
            default:
                apiClient.setPlaidAdapter(ApiClient.Sandbox);
                break;
        }
        
        // Create and return PlaidApi service
        return apiClient.createService(PlaidApi.class);
    }
}
