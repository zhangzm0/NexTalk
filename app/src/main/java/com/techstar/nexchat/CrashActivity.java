package com.techstar.nexchat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {

    private TextView tvError;
    private Button btnCopy, btnRestart, btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 使用简单的布局，避免可能的布局问题
        setContentView(android.R.layout.simple_list_item_1);

        // 直接显示错误信息
        String errorInfo = getIntent().getStringExtra("error_info");
        if (errorInfo != null) {
            TextView textView = findViewById(android.R.id.text1);
            textView.setText(errorInfo);
            textView.setTextColor(0xFFFFFFFF);
            textView.setBackgroundColor(0xFF000000);
            textView.setPadding(20, 20, 20, 20);

            // 自动复制到剪贴板
            copyToClipboard(errorInfo);
        }

        // 添加退出按钮
        addExitButton();
    }

    private void addExitButton() {
        Button exitButton = new Button(this);
        exitButton.setText("退出应用");
        exitButton.setTextColor(0xFFFFFFFF);
        exitButton.setBackgroundColor(0xFFFF0000);
        exitButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
					android.os.Process.killProcess(android.os.Process.myPid());
					System.exit(0);
				}
			});

        // 添加到布局
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF000000);

        TextView textView = findViewById(android.R.id.text1);
        android.view.ViewGroup parent = (android.view.ViewGroup) textView.getParent();
        parent.removeView(textView);

        layout.addView(textView);
        layout.addView(exitButton);

        setContentView(layout);
    }

    private void copyToClipboard(String text) {
        try {
            android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("错误信息", text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "错误信息已复制到剪贴板", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
