package com.techstar.nexchat.model;

import java.util.ArrayList;
import java.util.List;

public class ApiProvider {
    private String id;
    private String name;
    private String apiUrl;
    private String apiKey;
    private List<String> models;
    private String balance; // 余额信息
    private long createTime;

    public ApiProvider() {
        this.models = new ArrayList<String>();
        this.createTime = System.currentTimeMillis();
        this.balance = "未查询";
    }
	
	
	

    public ApiProvider(String name, String apiUrl, String apiKey) {
        this();
        this.name = name;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    // Getter and Setter 方法
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
	
	public String getBalance() { return balance; }
    public void setBalance(String balance) { this.balance = balance; }
}
