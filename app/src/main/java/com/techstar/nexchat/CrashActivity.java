package com.techstar.nexchat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

public class CrashActivity extends AppCompatActivity {

    private TextView tvError;
    private Button btnCopy, btnRestart, btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        initViews();
        setupClickListeners();

        // 显示错误信息
        String errorInfo = getIntent().getStringExtra("error_info");
        if (errorInfo != null) {
            tvError.setText(errorInfo);
            // 自动复制到剪贴板
            copyToClipboard(errorInfo);
        }
    }

    private void initViews() {
        tvError = findViewById(R.id.tvError);
        btnCopy = findViewById(R.id.btnCopy);
        btnRestart = findViewById(R.id.btnRestart);
        btnExit = findViewById(R.id.btnExit);
    }

    private void setupClickListeners() {
        btnCopy.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String errorInfo = tvError.getText().toString();
					copyToClipboard(errorInfo);
				}
			});

        btnRestart.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					restartApp();
				}
			});

        btnExit.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exitApp();
				}
			});
    }

    private void copyToClipboard(String text) {
        try {
            android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("错误信息", text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "错误信息已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restartApp() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exitApp() {
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}
