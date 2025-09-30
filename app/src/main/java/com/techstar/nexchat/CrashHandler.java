package com.techstar.nexchat;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        Log.d(TAG, "CrashHandler initialized");
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            // 1. 获取错误信息
            String errorInfo = getErrorInfo(ex);

            // 2. 写入日志文件（这是唯一要做的事）
            writeCrashLog(errorInfo);

            // 3. 输出到Logcat
            Log.e(TAG, "App Crashed: " + ex.getMessage(), ex);

        } catch (Exception e) {
            // 如果写文件也失败了，至少输出到Logcat
            Log.e(TAG, "CrashHandler itself failed: " + e.getMessage());
        } finally {
            // 4. 调用系统默认处理（杀死进程）
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
        sb.append("=== Crash Report ===\n");
        sb.append("Time: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        sb.append("App: NexChat\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("\nStack Trace:\n");
        sb.append(sw.toString());
        sb.append("\n=== End Report ===\n");

        return sb.toString();
    }

    private void writeCrashLog(String errorInfo) {
        FileOutputStream fos = null;
        try {
            // 获取外部存储目录（不需要权限的内部存储）
            File dir = context.getExternalFilesDir("crash_logs");
            if (dir == null) {
                dir = context.getFilesDir(); // 备用目录
            }

            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 创建日志文件，以时间戳命名
            String fileName = "crash_" + System.currentTimeMillis() + ".log";
            File logFile = new File(dir, fileName);

            fos = new FileOutputStream(logFile);
            fos.write(errorInfo.getBytes());
            fos.flush();

            Log.d(TAG, "Crash log saved: " + logFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Failed to write crash log: " + e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to close file stream: " + e.getMessage());
                }
            }
        }
    }

    // 可选：手动记录错误（用于非崩溃错误）
    public void logError(String message) {
        try {
            String logContent = "=== Error Log ===\n" +
				"Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n" +
				"Message: " + message + "\n" +
				"=== End Log ===\n";

            writeCrashLog(logContent);
            Log.d(TAG, "Error logged: " + message);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log error: " + e.getMessage());
        }
    }

    // 可选：获取最近的崩溃日志
    public String getLatestCrashLog() {
        try {
            File dir = context.getExternalFilesDir("crash_logs");
            if (dir == null || !dir.exists()) {
                return "No crash logs found";
            }

            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return "No crash logs found";
            }

            // 按修改时间排序，获取最新的文件
            File latestFile = null;
            for (File file : files) {
                if (file.getName().startsWith("crash_")) {
                    if (latestFile == null || file.lastModified() > latestFile.lastModified()) {
                        latestFile = file;
                    }
                }
            }

            if (latestFile != null) {
                java.io.FileInputStream fis = new java.io.FileInputStream(latestFile);
                byte[] data = new byte[(int) latestFile.length()];
                fis.read(data);
                fis.close();
                return new String(data);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to read crash log: " + e.getMessage());
        }
        return "Error reading crash log";
    }
}
