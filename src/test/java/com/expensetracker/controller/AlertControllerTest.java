package com.expensetracker.controller;

import com.expensetracker.model.Alert;
import com.expensetracker.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AlertControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AlertService alertService;
    
    @Test
    @WithMockUser(username = "testuser")
    public void testGetAlerts() throws Exception {
        Alert alert1 = new Alert("testuser", "WARNING", new BigDecimal("1000.00"), 
            new BigDecimal("850.00"), new BigDecimal("85.00"), "2024-01");
        alert1.setId(UUID.randomUUID().toString());
        
        Alert alert2 = new Alert("testuser", "CRITICAL", new BigDecimal("1000.00"), 
            new BigDecimal("1100.00"), new BigDecimal("110.00"), "2024-01");
        alert2.setId(UUID.randomUUID().toString());
        
        List<Alert> alerts = Arrays.asList(alert1, alert2);
        
        when(alertService.getAlerts("testuser"))
            .thenReturn(alerts);
        when(alertService.getUnreadAlertCount("testuser"))
            .thenReturn(2L);
        
        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alerts").isArray())
            .andExpect(jsonPath("$.alerts.length()").value(2))
            .andExpect(jsonPath("$.unreadCount").value(2));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    public void testDismissAlert() throws Exception {
        String alertId = UUID.randomUUID().toString();
        
        mockMvc.perform(put("/api/alerts/" + alertId + "/dismiss"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    public void testGetAlertsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isForbidden());
    }
}
