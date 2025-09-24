package com.techstar.nexchat;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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
        Log.d(TAG, "崩溃捕获已初始化");
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Log.e(TAG, "捕获到未处理异常", ex);

        try {
            // 获取错误信息
            String errorInfo = getErrorInfo(ex);

            // 显示错误对话框
            showCrashDialog(errorInfo);

            // 等待对话框显示
            Thread.sleep(3000);

        } catch (Exception e) {
            Log.e(TAG, "处理崩溃时出错", e);
        } finally {
            // 退出应用
            Process.killProcess(Process.myPid());
            System.exit(10);
        }
    }

    private String getErrorInfo(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.close();

        return "设备: " + Build.MANUFACTURER + " " + Build.MODEL + 
			"\nAndroid: " + Build.VERSION.RELEASE + 
			"\nSDK: " + Build.VERSION.SDK_INT +
			"\n\n错误详情:\n" + sw.toString();
    }

    private void showCrashDialog(final String errorInfo) {
        // 必须在主线程显示对话框
        new android.os.Handler(context.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					try {
						// 尝试启动崩溃Activity
						Intent intent = new Intent(context, CrashActivity.class);
						intent.putExtra("error_info", errorInfo);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(intent);
					} catch (Exception e) {
						// 如果启动Activity失败，显示系统对话框
						showSimpleDialog(errorInfo);
					}
				}
			});
    }

    private void showSimpleDialog(String errorInfo) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getValidContext());
            builder.setTitle("程序崩溃");
            builder.setMessage("应用遇到错误，即将退出。\n错误信息已复制到剪贴板。");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 什么都不做，直接退出
					}
				});
            builder.setCancelable(false);

            AlertDialog dialog = builder.create();
            dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            dialog.show();

            // 复制错误信息
            copyToClipboard(errorInfo);

        } catch (Exception e) {
            Log.e(TAG, "显示崩溃对话框失败", e);
        }
    }

    private Context getValidContext() {
        // 尝试获取有效的Context
        if (context != null) {
            return context;
        }
        // 如果ApplicationContext不可用，尝试其他方式
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Object context = activityThreadClass.getMethod("getApplication").invoke(activityThread);
            return (Context) context;
        } catch (Exception e) {
            return null;
        }
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("崩溃信息", text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
        } catch (Exception e) {
            Log.e(TAG, "复制到剪贴板失败", e);
        }
    }
}
