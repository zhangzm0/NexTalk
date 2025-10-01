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

    // 保存原始Log方法的引用
    private static java.lang.reflect.Method originalLogE;
    private static java.lang.reflect.Method originalLogW;
    private static java.lang.reflect.Method originalLogI;
    private static java.lang.reflect.Method originalLogD;

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

        // 拦截系统Log调用
        interceptSystemLogs();

        Log.d(TAG, "AppLogger initialized and intercepting system logs");
    }

    // 拦截系统Log调用
    private void interceptSystemLogs() {
        try {
            // 获取Log类的Class对象
            Class<?> logClass = Log.class;

            // 保存原始方法
            originalLogE = logClass.getMethod("e", String.class, String.class);
            originalLogW = logClass.getMethod("w", String.class, String.class);
            originalLogI = logClass.getMethod("i", String.class, String.class);
            originalLogD = logClass.getMethod("d", String.class, String.class);

            // 这里不能直接替换Log类的方法，因为它是final的
            // 所以我们采用另一种方式：在所有代码中使用我们自己的日志方法

        } catch (Exception e) {
            Log.e(TAG, "Failed to intercept system logs: " + e.getMessage());
        }
    }

    // 记录错误日志
    public static void e(String tag, String message) {
        // 1. 先输出到系统Logcat
        Log.e(tag, message);

        // 2. 写入文件
        if (getInstance().isInitialized) {
            getInstance().writeToFile("ERROR", tag, message, null);
        }
    }

    // 记录错误日志（带异常）
    public static void e(String tag, String message, Throwable tr) {
        // 1. 先输出到系统Logcat
        Log.e(tag, message, tr);

        // 2. 写入文件
        if (getInstance().isInitialized) {
            getInstance().writeToFile("ERROR", tag, message, tr);
        }
    }

    // 记录警告日志
    public static void w(String tag, String message) {
        Log.w(tag, message);
        if (getInstance().isInitialized) {
            getInstance().writeToFile("WARN", tag, message, null);
        }
    }

    // 记录信息日志
    public static void i(String tag, String message) {
        Log.i(tag, message);
        if (getInstance().isInitialized) {
            getInstance().writeToFile("INFO", tag, message, null);
        }
    }

    // 记录调试日志
    public static void d(String tag, String message) {
        Log.d(tag, message);
        if (getInstance().isInitialized) {
            getInstance().writeToFile("DEBUG", tag, message, null);
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
}
