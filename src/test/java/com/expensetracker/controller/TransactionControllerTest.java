package com.expensetracker.controller;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionRequest;
import com.expensetracker.service.TransactionService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TransactionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TransactionService transactionService;
    
    @Test
    @WithMockUser(username = "testuser")
    public void testCreateTransaction() throws Exception {
        TransactionRequest request = new TransactionRequest(
            new BigDecimal("50.00"),
            LocalDate.now(),
            "Test transaction",
            "Food"
        );
        
        Transaction transaction = new Transaction("testuser", request.getAmount(), 
            request.getDate(), request.getDescription(), request.getCategory());
        
        when(transactionService.createTransaction(eq("testuser"), any(TransactionRequest.class)))
            .thenReturn(transaction);
        
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(50.00))
            .andExpect(jsonPath("$.description").value("Test transaction"))
            .andExpect(jsonPath("$.category").value("Food"));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testGetAllTransactions() throws Exception {
        Transaction transaction1 = new Transaction("testuser", new BigDecimal("50.00"), 
            LocalDate.now(), "Transaction 1", "Food");
        Transaction transaction2 = new Transaction("testuser", new BigDecimal("30.00"), 
            LocalDate.now(), "Transaction 2", "Transportation");
        
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);
        
        when(transactionService.getAllTransactions("testuser"))
            .thenReturn(transactions);
        
        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactions").isArray())
            .andExpect(jsonPath("$.transactions.length()").value(2))
            .andExpect(jsonPath("$.total").value(80.00));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testUpdateTransaction() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        TransactionRequest request = new TransactionRequest(
            new BigDecimal("75.00"),
            LocalDate.now(),
            "Updated transaction",
            "Entertainment"
        );
        
        Transaction transaction = new Transaction("testuser", request.getAmount(), 
            request.getDate(), request.getDescription(), request.getCategory());
        
        when(transactionService.updateTransaction(eq(transactionId), eq("testuser"), any(TransactionRequest.class)))
            .thenReturn(transaction);
        
        mockMvc.perform(put("/api/transactions/" + transactionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(75.00))
            .andExpect(jsonPath("$.description").value("Updated transaction"))
            .andExpect(jsonPath("$.category").value("Entertainment"));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testDeleteTransaction() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        
        mockMvc.perform(delete("/api/transactions/" + transactionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testGetTransactionsByCategory() throws Exception {
        Transaction transaction1 = new Transaction("testuser", new BigDecimal("50.00"), 
            LocalDate.now(), "Transaction 1", "Food");
        Transaction transaction2 = new Transaction("testuser", new BigDecimal("30.00"), 
            LocalDate.now(), "Transaction 2", "Food");
        
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);
        
        when(transactionService.getTransactionsByCategory("testuser", "Food"))
            .thenReturn(transactions);
        
        mockMvc.perform(get("/api/transactions/by-category")
                .param("category", "Food"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactions").isArray())
            .andExpect(jsonPath("$.transactions.length()").value(2));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testGetCategoryTotals() throws Exception {
        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        categoryTotals.put("Food", new BigDecimal("150.00"));
        categoryTotals.put("Transportation", new BigDecimal("75.00"));
        
        when(transactionService.getTotalsByCategory(eq("testuser"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(categoryTotals);
        
        mockMvc.perform(get("/api/transactions/by-category"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryTotals.Food").value(150.00))
            .andExpect(jsonPath("$.categoryTotals.Transportation").value(75.00));
    }
    
    @Test
    public void testCreateTransactionWithoutAuthentication() throws Exception {
        TransactionRequest request = new TransactionRequest(
            new BigDecimal("50.00"),
            LocalDate.now(),
            "Test transaction",
            "Food"
        );
        
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden()); // Spring Security returns 403 for unauthenticated requests
    }
}
