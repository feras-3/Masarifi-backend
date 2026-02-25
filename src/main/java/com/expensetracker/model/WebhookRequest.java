package com.expensetracker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookRequest {
    
    @JsonProperty("webhook_type")
    private String webhookType;
    
    @JsonProperty("webhook_code")
    private String webhookCode;
    
    @JsonProperty("item_id")
    private String itemId;
    
    private WebhookError error;
    
    public WebhookRequest() {
    }
    
    // Getters and Setters
    
    public String getWebhookType() {
        return webhookType;
    }
    
    public void setWebhookType(String webhookType) {
        this.webhookType = webhookType;
    }
    
    public String getWebhookCode() {
        return webhookCode;
    }
    
    public void setWebhookCode(String webhookCode) {
        this.webhookCode = webhookCode;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public WebhookError getError() {
        return error;
    }
    
    public void setError(WebhookError error) {
        this.error = error;
    }
    
    public static class WebhookError {
        @JsonProperty("error_code")
        private String errorCode;
        
        @JsonProperty("error_message")
        private String errorMessage;
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
