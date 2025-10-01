package com.techstar.nexchat;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppLogger {

    private static final String TAG = "AppLogger";
    private static AppLogger instance;
    private Context context;
    private boolean isInitialized = false;
    private SimpleDateFormat dateFormat;

    public static AppLogger getInstance() {
        if (instance == null) {
            instance = new AppLogger();
        }
        return instance;
    }

    private AppLogger() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.isInitialized = true;
        Log.d(TAG, "AppLogger initialized");
    }

    // 记录错误日志
    public void e(String tag, String message) {
        // 1. 先输出到系统Logcat
        Log.e(tag, message);

        // 2. 写入文件
        if (isInitialized) {
            writeToFile("ERROR", tag, message, null);
        }
    }

    // 记录错误日志（带异常）
    public void e(String tag, String message, Throwable tr) {
        // 1. 先输出到系统Logcat
        Log.e(tag, message, tr);

        // 2. 写入文件
        if (isInitialized) {
            writeToFile("ERROR", tag, message, tr);
        }
    }

    // 记录警告日志
    public void w(String tag, String message) {
        Log.w(tag, message);
        if (isInitialized) {
            writeToFile("WARN", tag, message, null);
        }
    }

    // 记录信息日志
    public void i(String tag, String message) {
        Log.i(tag, message);
        if (isInitialized) {
            writeToFile("INFO", tag, message, null);
        }
    }

    // 记录调试日志
    public void d(String tag, String message) {
        Log.d(tag, message);
        if (isInitialized) {
            writeToFile("DEBUG", tag, message, null);
        }
    }

    // 写入文件的核心方法
    private synchronized void writeToFile(String level, String tag, String message, Throwable tr) {
        FileOutputStream fos = null;
        try {
            // 构建日志内容
            StringBuilder logContent = new StringBuilder();
            logContent.append(dateFormat.format(new Date()))
				.append(" [").append(level).append("] ")
				.append("[").append(tag).append("] ")
				.append(message);

            if (tr != null) {
                logContent.append("\n").append(getStackTraceString(tr));
            }
            logContent.append("\n");

            // 获取日志目录
            File dir = context.getExternalFilesDir("app_logs");
            if (dir == null) {
                dir = context.getFilesDir(); // 备用目录
            }

            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 按日期创建日志文件，每天一个文件
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            File logFile = new File(dir, "app_" + dateStr + ".log");

            // 追加写入文件
            fos = new FileOutputStream(logFile, true);
            fos.write(logContent.toString().getBytes());
            fos.flush();

        } catch (Exception e) {
            // 如果写文件失败，只输出到Logcat，避免循环错误
            Log.e(TAG, "Failed to write log to file: " + e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to close log file: " + e.getMessage());
                }
            }
        }
    }

    // 获取异常堆栈字符串
    private String getStackTraceString(Throwable tr) {
        if (tr == null) return "";

        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    // 获取今天的日志文件内容
    public String getTodayLogs() {
        try {
            File dir = context.getExternalFilesDir("app_logs");
            if (dir == null || !dir.exists()) {
                return "No log directory";
            }

            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            File logFile = new File(dir, "app_" + dateStr + ".log");

            if (!logFile.exists()) {
                return "No logs for today";
            }

            java.io.FileInputStream fis = new java.io.FileInputStream(logFile);
            byte[] data = new byte[(int) logFile.length()];
            fis.read(data);
            fis.close();
            return new String(data);

        } catch (Exception e) {
            return "Error reading logs: " + e.getMessage();
        }
    }

    // 清理旧日志文件（保留最近7天）
    public void cleanupOldLogs() {
        try {
            File dir = context.getExternalFilesDir("app_logs");
            if (dir == null || !dir.exists()) {
                return;
            }

            File[] files = dir.listFiles();
            if (files == null) return;

            long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);

            for (File file : files) {
                if (file.getName().startsWith("app_") && file.getName().endsWith(".log")) {
                    if (file.lastModified() < sevenDaysAgo) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            Log.d(TAG, "Deleted old log file: " + file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup old logs: " + e.getMessage());
        }
    }
}
