package com.techstar.nexchat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.techstar.nexchat.util.FileLogger;

public class CrashActivity extends AppCompatActivity {
    private static final String TAG = "CrashActivity";
    
    private TextView tvError;
    private Button btnCopy;
    private Button btnRestart;
    private Button btnExit;
    private FileLogger logger;
    
    private String crashInfo;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);
        
        logger = FileLogger.getInstance(this);
        logger.i(TAG, "CrashActivity created");
        
        initViews();
        loadCrashInfo();
    }
    
    private void initViews() {
        tvError = findViewById(R.id.tvError);
        btnCopy = findViewById(R.id.btnCopy);
        btnRestart = findViewById(R.id.btnRestart);
        btnExit = findViewById(R.id.btnExit);
        
        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyErrorToClipboard();
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
    
    private void loadCrashInfo() {
        crashInfo = getIntent().getStringExtra("crash_info");
        if (crashInfo != null) {
            tvError.setText(crashInfo);
            logger.e(TAG, "Crash info displayed:\n" + crashInfo);
        } else {
            crashInfo = "No crash information available";
            tvError.setText(crashInfo);
            logger.w(TAG, "No crash info provided");
        }
    }
    
    private void copyErrorToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Log", crashInfo);
            clipboard.setPrimaryClip(clip);
            
            android.widget.Toast.makeText(this, "错误日志已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show();
            logger.i(TAG, "Crash log copied to clipboard");
        } catch (Exception e) {
            logger.e(TAG, "Failed to copy crash log to clipboard", e);
            android.widget.Toast.makeText(this, "复制失败", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void restartApp() {
        logger.i(TAG, "Restarting application");
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            
            // 结束进程以确保完全重启
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } catch (Exception e) {
            logger.e(TAG, "Failed to restart app", e);
            // 如果重启失败，直接退出
            exitApp();
        }
    }
    
    private void exitApp() {
        logger.i(TAG, "Exiting application");
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
    
    @Override
    public void onBackPressed() {
        // 禁用返回键，防止用户返回到崩溃的状态
        // super.onBackPressed();
        android.widget.Toast.makeText(this, "请选择重启或退出", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        logger.i(TAG, "CrashActivity destroyed");
    }
}