package com.techstar.nexchat.model;

import java.util.List;

public class ApiProvider {
    private int id;
    private String name;
    private String apiUrl;
    private String apiKey;
    private List<String> models;
    private String balance;
    private long createdAt;
    
    public ApiProvider() {}
    
    public ApiProvider(String name, String apiUrl, String apiKey) {
        this.name = name;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Getter and Setter methods
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }
    
    public String getBalance() { return balance; }
    public void setBalance(String balance) { this.balance = balance; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}