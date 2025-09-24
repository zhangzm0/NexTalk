package com.techstar.nexchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SettingsActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        CardView apiProviderCard = findViewById(R.id.apiProviderLayout);
        apiProviderCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SettingsActivity.this, ApiProvidersActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SettingsActivity.this, "打开失败: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
