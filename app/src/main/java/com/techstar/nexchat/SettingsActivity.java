package com.techstar.nexchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        CardView apiProviderCard = findViewById(R.id.apiProviderLayout);
        apiProviderCard.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(SettingsActivity.this, ApiProvidersActivity.class);
					startActivity(intent);
				}
			});
    }
}
