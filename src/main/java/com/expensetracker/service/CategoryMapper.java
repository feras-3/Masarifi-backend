package com.expensetracker.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for mapping Plaid transaction categories to application categories
 * Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7, 15.8
 */
@Service
public class CategoryMapper {
    
    private final Map<String, String> categoryMappings;
    
    public CategoryMapper() {
        this.categoryMappings = new HashMap<>();
        initializeMappings();
    }
    
    /**
     * Initialize category mappings from Plaid categories to application categories
     */
    private void initializeMappings() {
        // Food and Dining categories -> Food
        categoryMappings.put("Food and Drink", "Food");
        categoryMappings.put("Restaurants", "Food");
        categoryMappings.put("Fast Food", "Food");
        categoryMappings.put("Coffee Shop", "Food");
        categoryMappings.put("Bar", "Food");
        categoryMappings.put("Groceries", "Food");
        
        // Transportation categories -> Transportation
        categoryMappings.put("Transportation", "Transportation");
        categoryMappings.put("Gas", "Transportation");
        categoryMappings.put("Parking", "Transportation");
        categoryMappings.put("Public Transportation", "Transportation");
        categoryMappings.put("Taxi", "Transportation");
        categoryMappings.put("Ride Share", "Transportation");
        categoryMappings.put("Tolls", "Transportation");
        categoryMappings.put("Auto & Transport", "Transportation");
        
        // Entertainment and Recreation categories -> Entertainment
        categoryMappings.put("Entertainment", "Entertainment");
        categoryMappings.put("Recreation", "Entertainment");
        categoryMappings.put("Arts", "Entertainment");
        categoryMappings.put("Music", "Entertainment");
        categoryMappings.put("Movies & DVDs", "Entertainment");
        categoryMappings.put("Sporting Events", "Entertainment");
        categoryMappings.put("Amusement", "Entertainment");
        categoryMappings.put("Games", "Entertainment");
        
        // Utilities and Bills categories -> Utilities
        categoryMappings.put("Utilities", "Utilities");
        categoryMappings.put("Bills & Utilities", "Utilities");
        categoryMappings.put("Internet", "Utilities");
        categoryMappings.put("Cable", "Utilities");
        categoryMappings.put("Phone", "Utilities");
        categoryMappings.put("Mobile Phone", "Utilities");
        categoryMappings.put("Electric", "Utilities");
        categoryMappings.put("Gas & Electric", "Utilities");
        categoryMappings.put("Water", "Utilities");
        
        // Healthcare and Medical categories -> Healthcare
        categoryMappings.put("Healthcare", "Healthcare");
        categoryMappings.put("Health & Fitness", "Healthcare");
        categoryMappings.put("Doctor", "Healthcare");
        categoryMappings.put("Dentist", "Healthcare");
        categoryMappings.put("Pharmacy", "Healthcare");
        categoryMappings.put("Eyecare", "Healthcare");
        categoryMappings.put("Medical", "Healthcare");
        categoryMappings.put("Gym", "Healthcare");
        categoryMappings.put("Sports", "Healthcare");
        
        // Shopping and Retail categories -> Shopping
        categoryMappings.put("Shopping", "Shopping");
        categoryMappings.put("Retail", "Shopping");
        categoryMappings.put("Clothing", "Shopping");
        categoryMappings.put("Electronics & Software", "Shopping");
        categoryMappings.put("Sporting Goods", "Shopping");
        categoryMappings.put("Bookstores", "Shopping");
        categoryMappings.put("Hobbies", "Shopping");
        categoryMappings.put("General Merchandise", "Shopping");
    }
    
    /**
     * Map a Plaid category to an application category
     * Returns "Other" if no mapping is found
     * Requirements: 15.1, 15.8
     */
    public String mapPlaidCategory(String plaidCategory) {
        if (plaidCategory == null || plaidCategory.isEmpty()) {
            return "Other";
        }
        
        // Try exact match first
        String mapped = categoryMappings.get(plaidCategory);
        if (mapped != null) {
            return mapped;
        }
        
        // Try partial match (case-insensitive)
        String lowerPlaidCategory = plaidCategory.toLowerCase();
        for (Map.Entry<String, String> entry : categoryMappings.entrySet()) {
            if (lowerPlaidCategory.contains(entry.getKey().toLowerCase()) ||
                entry.getKey().toLowerCase().contains(lowerPlaidCategory)) {
                return entry.getValue();
            }
        }
        
        // Default to Other if no mapping found
        return "Other";
    }
    
    /**
     * Get all category mappings
     */
    public Map<String, String> getAllMappings() {
        return new HashMap<>(categoryMappings);
    }
}
