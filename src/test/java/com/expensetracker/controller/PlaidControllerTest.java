package com.expensetracker.controller;

import com.expensetracker.model.PlaidAccount;
import com.expensetracker.model.TransactionSyncResult;
import com.expensetracker.model.WebhookRequest;
import com.expensetracker.service.PlaidService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PlaidControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private PlaidService plaidService;
    
    @Test
    public void testCreatePublicToken() throws Exception {
        String publicToken = "public-sandbox-test-token";
        String institutionId = "ins_109508";
        
        when(plaidService.createPublicToken(eq(institutionId)))
            .thenReturn(publicToken);
        
        Map<String, String> request = new HashMap<>();
        request.put("institutionId", institutionId);
        
        mockMvc.perform(post("/api/plaid/public-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicToken").value(publicToken));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testExchangePublicToken() throws Exception {
        String publicToken = "public-sandbox-test-token";
        PlaidAccount account = new PlaidAccount("testuser", "encrypted-access-token", 
            "item-123", "Test Bank");
        account.setId(UUID.randomUUID().toString());
        
        when(plaidService.exchangePublicToken(eq(publicToken), eq("testuser")))
            .thenReturn(account);
        
        Map<String, String> request = new HashMap<>();
        request.put("publicToken", publicToken);
        
        mockMvc.perform(post("/api/plaid/exchange-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.institutionName").value("Test Bank"));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testSyncTransactions() throws Exception {
        TransactionSyncResult result = new TransactionSyncResult();
        result.setSuccess(true);
        result.setNewTransactionCount(5);
        
        when(plaidService.syncTransactions("testuser"))
            .thenReturn(result);
        
        mockMvc.perform(post("/api/plaid/sync-transactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.newTransactionCount").value(5));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testGetLinkedAccounts() throws Exception {
        PlaidAccount account = new PlaidAccount("testuser", "encrypted-access-token", 
            "item-123", "Test Bank");
        account.setId(UUID.randomUUID().toString());
        account.setIsActive(true);
        
        List<PlaidAccount> accounts = Arrays.asList(account);
        
        when(plaidService.getLinkedAccounts("testuser"))
            .thenReturn(accounts);
        
        mockMvc.perform(get("/api/plaid/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accounts").isArray())
            .andExpect(jsonPath("$.accounts.length()").value(1))
            .andExpect(jsonPath("$.linked").value(true));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testUnlinkAccount() throws Exception {
        mockMvc.perform(delete("/api/plaid/unlink"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    public void testHandleWebhook() throws Exception {
        WebhookRequest webhookRequest = new WebhookRequest();
        webhookRequest.setWebhookType("TRANSACTIONS");
        webhookRequest.setWebhookCode("DEFAULT_UPDATE");
        webhookRequest.setItemId("item-123");
        
        mockMvc.perform(post("/api/plaid/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.acknowledged").value(true));
    }
    
    @Test
    public void testCreatePublicTokenWithoutInstitutionId() throws Exception {
        Map<String, String> request = new HashMap<>();
        // Missing institutionId
        
        mockMvc.perform(post("/api/plaid/public-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("institutionId is required"));
    }
}
