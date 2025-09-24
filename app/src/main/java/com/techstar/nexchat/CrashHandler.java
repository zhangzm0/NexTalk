package com.techstar.nexchat;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

public class CrashHandler implements UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static CrashHandler instance;
    private Context context;
    private UncaughtExceptionHandler defaultHandler;

    public static CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            // 获取错误信息
            String errorInfo = getErrorInfo(ex);
            Log.e(TAG, "程序崩溃", ex);

            // 在主线程显示错误对话框
            showCrashDialog(errorInfo);

            // 等待一下让对话框显示出来
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 调用系统默认处理
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex);
            } else {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        }
    }

    private String getErrorInfo(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.close();

        StringBuilder sb = new StringBuilder();
        sb.append("设备信息:\n");
        sb.append("Android版本: ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("SDK版本: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("设备型号: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("\n错误信息:\n");
        sb.append(sw.toString());

        return sb.toString();
    }

    private void showCrashDialog(final String errorInfo) {
        // 在主线程显示对话框
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(new Runnable() {
				@Override
				public void run() {
					try {
						// 尝试启动崩溃Activity
						Intent intent = new Intent(context, CrashActivity.class);
						intent.putExtra("error_info", errorInfo);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
						context.startActivity(intent);
					} catch (Exception e) {
						// 如果启动自定义Activity失败，显示系统对话框
						showSystemDialog(errorInfo);
					}
				}
			});
    }

    private void showSystemDialog(final String errorInfo) {
        try {
            // 复制错误信息到剪贴板
            copyToClipboard(errorInfo);

            // 显示简单提示
            android.widget.Toast.makeText(context, "程序崩溃，错误信息已复制到剪贴板", android.widget.Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("错误信息", text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
