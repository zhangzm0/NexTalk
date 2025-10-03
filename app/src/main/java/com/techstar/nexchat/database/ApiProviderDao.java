package com.techstar.nexchat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.util.FileLogger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ApiProviderDao {
    private static final String TAG = "ApiProviderDao";
    private DatabaseHelper dbHelper;
    private FileLogger logger;
    private Gson gson;
    
    public ApiProviderDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.logger = FileLogger.getInstance(context);
        this.gson = new Gson();
    }
    
    public long insertProvider(ApiProvider provider) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;
        
        try {
            ContentValues values = new ContentValues();
            values.put("name", provider.getName());
            values.put("api_url", provider.getApiUrl());
            values.put("api_key", provider.getApiKey());
            
            // 将 models 列表转为 JSON 字符串存储
            if (provider.getModels() != null) {
                String modelsJson = gson.toJson(provider.getModels());
                values.put("models", modelsJson);
            }
            
            if (provider.getBalance() != null) {
                values.put("balance", provider.getBalance());
            }
            
            values.put("created_at", provider.getCreatedAt());
            
            id = db.insert(DatabaseHelper.TABLE_API_PROVIDERS, null, values);
            logger.i(TAG, "Inserted API provider: " + provider.getName() + " with ID: " + id);
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to insert API provider", e);
        } finally {
            db.close();
        }
        
        return id;
    }
    
    public List<ApiProvider> getAllProviders() {
        List<ApiProvider> providers = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(DatabaseHelper.TABLE_API_PROVIDERS, 
                    null, null, null, null, null, "created_at DESC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ApiProvider provider = cursorToProvider(cursor);
                    providers.add(provider);
                } while (cursor.moveToNext());
            }
            
            logger.i(TAG, "Retrieved " + providers.size() + " API providers");
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to retrieve API providers", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return providers;
    }
    
    public ApiProvider getProviderById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        ApiProvider provider = null;
        
        try {
            cursor = db.query(DatabaseHelper.TABLE_API_PROVIDERS, 
                    null, "id = ?", new String[]{String.valueOf(id)}, 
                    null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                provider = cursorToProvider(cursor);
            }
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to get provider by ID: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return provider;
    }
    
    public boolean updateProvider(ApiProvider provider) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;
        
        try {
            ContentValues values = new ContentValues();
            values.put("name", provider.getName());
            values.put("api_url", provider.getApiUrl());
            values.put("api_key", provider.getApiKey());
            
            if (provider.getModels() != null) {
                String modelsJson = gson.toJson(provider.getModels());
                values.put("models", modelsJson);
            }
            
            if (provider.getBalance() != null) {
                values.put("balance", provider.getBalance());
            }
            
            int rowsAffected = db.update(DatabaseHelper.TABLE_API_PROVIDERS, 
                    values, "id = ?", new String[]{String.valueOf(provider.getId())});
            
            success = rowsAffected > 0;
            logger.i(TAG, "Updated API provider: " + provider.getName() + ", rows affected: " + rowsAffected);
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to update API provider", e);
        } finally {
            db.close();
        }
        
        return success;
    }
    
    public boolean deleteProvider(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;
        
        try {
            int rowsAffected = db.delete(DatabaseHelper.TABLE_API_PROVIDERS, 
                    "id = ?", new String[]{String.valueOf(id)});
            
            success = rowsAffected > 0;
            logger.i(TAG, "Deleted API provider with ID: " + id + ", rows affected: " + rowsAffected);
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to delete API provider with ID: " + id, e);
        } finally {
            db.close();
        }
        
        return success;
    }
    
    private ApiProvider cursorToProvider(Cursor cursor) {
        ApiProvider provider = new ApiProvider();
        provider.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        provider.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        provider.setApiUrl(cursor.getString(cursor.getColumnIndexOrThrow("api_url")));
        provider.setApiKey(cursor.getString(cursor.getColumnIndexOrThrow("api_key")));
        provider.setBalance(cursor.getString(cursor.getColumnIndexOrThrow("balance")));
        provider.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        
        // 解析 models JSON 字符串
        String modelsJson = cursor.getString(cursor.getColumnIndexOrThrow("models"));
        if (modelsJson != null && !modelsJson.isEmpty()) {
            try {
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> models = gson.fromJson(modelsJson, listType);
                provider.setModels(models);
            } catch (Exception e) {
                logger.e(TAG, "Failed to parse models JSON: " + modelsJson, e);
            }
        }
        
        return provider;
    }
}