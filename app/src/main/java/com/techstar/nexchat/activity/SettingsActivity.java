package com.techstar.nexchat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.techstar.nexchat.util.FileLogger;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    
    private FileLogger logger;
    private ImageButton btnSettingBack;
    private CardView apiProviderLayout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        logger = FileLogger.getInstance(this);
        logger.i(TAG, "SettingsActivity created");
        
        initViews();
        setupClickListeners();
    }
    
    private void initViews() {
        btnSettingBack = findViewById(R.id.btnSettingBack);
        apiProviderLayout = findViewById(R.id.apiProviderLayout);
    }
    
    private void setupClickListeners() {
        btnSettingBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        apiProviderLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openApiProviders();
            }
        });
    }
    
    private void openApiProviders() {
        logger.i(TAG, "Opening API providers");
        Intent intent = new Intent(this, ApiProvidersActivity.class);
        startActivity(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        logger.i(TAG, "SettingsActivity destroyed");
    }
}