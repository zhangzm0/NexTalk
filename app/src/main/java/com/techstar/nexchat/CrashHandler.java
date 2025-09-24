package com.techstar.nexchat;

import android.app.Activity;
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
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            // 获取错误信息
            String errorInfo = getErrorInfo(ex);
            Log.e(TAG, "程序崩溃", ex);

            // 保存错误日志
            saveCrashInfo(errorInfo);

            // 在主线程显示错误对话框
            if (context instanceof android.app.Application) {
                showCrashDialog(errorInfo);
            }

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

    private void saveCrashInfo(String errorInfo) {
        // 这里可以保存错误信息到文件
        Log.e(TAG, errorInfo);
    }

    private void showCrashDialog(final String errorInfo) {
        // 在主线程显示对话框
        new android.os.Handler(context.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					try {
						// 创建自定义的崩溃Activity
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getTopActivity());
            builder.setTitle("程序崩溃");
            builder.setMessage("很抱歉，程序出现错误，即将退出。\n\n错误信息已复制到剪贴板。");
            builder.setPositiveButton("复制错误", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						copyToClipboard(errorInfo);
					}
				});
            builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						System.exit(0);
					}
				});
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            dialog.show();

            // 自动复制错误信息
            copyToClipboard(errorInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Activity getTopActivity() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Object activities = activitiesField.get(activityThread);
            if (activities instanceof java.util.Map) {
                for (Object activityRecord : ((java.util.Map) activities).values()) {
                    Class<?> activityRecordClass = activityRecord.getClass();
                    java.lang.reflect.Field pausedField = activityRecordClass.getDeclaredField("paused");
                    pausedField.setAccessible(true);
                    if (!pausedField.getBoolean(activityRecord)) {
                        java.lang.reflect.Field activityField = activityRecordClass.getDeclaredField("activity");
                        activityField.setAccessible(true);
                        return (Activity) activityField.get(activityRecord);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("错误信息", text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                android.widget.Toast.makeText(context, "错误信息已复制", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
