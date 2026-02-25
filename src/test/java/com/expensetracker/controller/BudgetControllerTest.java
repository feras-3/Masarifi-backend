package com.expensetracker.controller;

import com.expensetracker.model.Budget;
import com.expensetracker.model.BudgetRequest;
import com.expensetracker.model.BudgetStatus;
import com.expensetracker.service.BudgetService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BudgetControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private BudgetService budgetService;
    
    @Test
    @WithMockUser(username = "testuser")
    public void testCreateBudget() throws Exception {
        BudgetRequest request = new BudgetRequest(new BigDecimal("1000.00"), "2024-01");
        
        Budget budget = new Budget("testuser", request.getAmount(), request.getPeriod());
        budget.setId(UUID.randomUUID().toString());
        
        when(budgetService.createBudget(eq("testuser"), any(BudgetRequest.class)))
            .thenReturn(budget);
        
        mockMvc.perform(post("/api/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(1000.00))
            .andExpect(jsonPath("$.period").value("2024-01"));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testGetCurrentBudget() throws Exception {
        BudgetStatus budgetStatus = new BudgetStatus(
            UUID.randomUUID().toString(),
            new BigDecimal("1000.00"),
            new BigDecimal("600.00"),
            new BigDecimal("400.00"),
            new BigDecimal("60.00"),
            "2024-01"
        );
        
        when(budgetService.getBudgetStatus("testuser"))
            .thenReturn(budgetStatus);
        
        mockMvc.perform(get("/api/budgets/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(1000.00))
            .andExpect(jsonPath("$.spent").value(600.00))
            .andExpect(jsonPath("$.remaining").value(400.00))
            .andExpect(jsonPath("$.percentageUsed").value(60.00));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testUpdateBudget() throws Exception {
        String budgetId = UUID.randomUUID().toString();
        BigDecimal newAmount = new BigDecimal("1500.00");
        
        Budget budget = new Budget("testuser", newAmount, "2024-01");
        budget.setId(budgetId);
        
        when(budgetService.updateBudget(eq(budgetId), eq("testuser"), eq(newAmount)))
            .thenReturn(budget);
        
        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", newAmount);
        
        mockMvc.perform(put("/api/budgets/" + budgetId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(1500.00));
    }
    
    @Test
    public void testCreateBudgetWithoutAuthentication() throws Exception {
        BudgetRequest request = new BudgetRequest(new BigDecimal("1000.00"), "2024-01");
        
        mockMvc.perform(post("/api/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }
}
