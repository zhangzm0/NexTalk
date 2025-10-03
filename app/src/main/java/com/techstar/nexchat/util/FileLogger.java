package com.techstar.nexchat.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger {
    private static final String TAG = "NexTalk";
    private static final String LOG_DIR = "NexTalk/Logs";
    private static final String LOG_FILE = "app_log.txt";
    private static final int MAX_LOG_SIZE = 2 * 1024 * 1024; // 2MB
    
    private static FileLogger instance;
    private Context context;
    private SimpleDateFormat dateFormat;
    
    private FileLogger(Context context) {
        this.context = context.getApplicationContext();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    }
    
    public static synchronized FileLogger getInstance(Context context) {
        if (instance == null) {
            instance = new FileLogger(context);
        }
        return instance;
    }
    
    public void d(String tag, String message) {
        Log.d(tag, message);
        writeToFile("DEBUG", tag, message);
    }
    
    public void i(String tag, String message) {
        Log.i(tag, message);
        writeToFile("INFO", tag, message);
    }
    
    public void w(String tag, String message) {
        Log.w(tag, message);
        writeToFile("WARN", tag, message);
    }
    
    public void e(String tag, String message) {
        Log.e(tag, message);
        writeToFile("ERROR", tag, message);
    }
    
    public void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        String stackTrace = getStackTraceString(throwable);
        writeToFile("ERROR", tag, message + "\n" + stackTrace);
    }
    
    private void writeToFile(String level, String tag, String message) {
        if (!isExternalStorageWritable()) {
            return;
        }
        
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("%s [%s] %s: %s\n", timestamp, level, tag, message);
        
        FileOutputStream outputStream = null;
        try {
            File logFile = getLogFile();
            if (logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile();
                logFile = getLogFile();
            }
            
            outputStream = new FileOutputStream(logFile, true);
            outputStream.write(logEntry.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log to file", e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close log file", e);
                }
            }
        }
    }
    
    private File getLogFile() throws IOException {
        File logDir = new File(Environment.getExternalStorageDirectory(), LOG_DIR);
        if (!logDir.exists() && !logDir.mkdirs()) {
            throw new IOException("Failed to create log directory: " + logDir.getAbsolutePath());
        }
        return new File(logDir, LOG_FILE);
    }
    
    private void rotateLogFile() {
        try {
            File currentLog = getLogFile();
            if (currentLog.exists()) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File backupLog = new File(currentLog.getParent(), "app_log_" + timestamp + ".txt");
                currentLog.renameTo(backupLog);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to rotate log file", e);
        }
    }
    
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    
    private String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    public String getLogFilePath() {
        try {
            return getLogFile().getAbsolutePath();
        } catch (IOException e) {
            return "Unable to get log file path";
        }
    }
    
    public void clearLogs() {
        try {
            File logFile = getLogFile();
            if (logFile.exists()) {
                // 清空文件内容
                FileOutputStream outputStream = new FileOutputStream(logFile);
                outputStream.write("".getBytes());
                outputStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to clear logs", e);
        }
    }
}