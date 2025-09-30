package com.techstar.nexchat;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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
	// ... 其他代码不变

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
				Thread.sleep(2000);
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

	private void showCrashDialog(final String errorInfo) {
		// 在主线程显示对话框
		android.os.Handler handler = new android.os.Handler(context.getMainLooper());
		handler.post(new Runnable() {
				@Override
				public void run() {
					try {
						// 直接显示系统对话框，避免Activity启动问题
						showSystemDialog(errorInfo);
					} catch (Exception e) {
						// 最后的备用方案：写入日志文件
						writeErrorToFile(errorInfo);
					}
				}
			});
	}

	private void showSystemDialog(final String errorInfo) {
		try {
			// 复制错误信息到剪贴板
			copyToClipboard(errorInfo);

			// 使用Application Context创建对话框
			android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getTopActivity());
			builder.setTitle("程序崩溃")
				.setMessage("很抱歉，程序出现错误。\n错误信息已复制到剪贴板。\n应用即将退出。")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						System.exit(0);
					}
				})
				.setCancelable(false);

			android.app.AlertDialog dialog = builder.create();

			// 设置对话框类型，确保能显示在其他应用上面
			dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
			dialog.show();

		} catch (Exception e) {
			// 如果对话框也显示失败，至少确保错误信息被复制
			copyToClipboard(errorInfo);
			writeErrorToFile(errorInfo);
		}
	}

	private Activity getTopActivity() {
		try {
			Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
			Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
			java.lang.reflect.Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
			activitiesField.setAccessible(true);

			Object activities = activitiesField.get(activityThread);
			if (activities instanceof android.util.ArrayMap) {
				android.util.ArrayMap<?, ?> arrayMap = (android.util.ArrayMap<?, ?>) activities;
				for (Object activityRecord : arrayMap.values()) {
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

	private void writeErrorToFile(String errorInfo) {
		try {
			java.io.File file = new java.io.File(context.getExternalFilesDir(null), "crash_log.txt");
			java.io.FileWriter writer = new java.io.FileWriter(file, true);
			writer.write("=== Crash Time: " + new java.util.Date() + " ===\n");
			writer.write(errorInfo);
			writer.write("\n\n");
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
