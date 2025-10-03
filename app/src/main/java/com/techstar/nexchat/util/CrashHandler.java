package com.techstar.nexchat.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import com.techstar.nexchat.CrashActivity;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

public class CrashHandler implements UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    
    private static CrashHandler instance;
    private Context context;
    private FileLogger logger;
    private UncaughtExceptionHandler defaultHandler;
    
    private CrashHandler() {}
    
    public static CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }
    
    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.logger = FileLogger.getInstance(context);
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        
        logger.i(TAG, "Crash handler initialized");
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logger.e(TAG, "Uncaught exception occurred", throwable);
        
        // 记录崩溃信息
        String crashInfo = getCrashInfo(thread, throwable);
        logger.e(TAG, crashInfo);
        
        // 启动崩溃页面
        Intent intent = new Intent(context, CrashActivity.class);
        intent.putExtra("crash_info", crashInfo);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            logger.e(TAG, "Failed to start crash activity", e);
        }
        
        // 等待一下确保崩溃页面启动
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.e(TAG, "Interrupted while waiting to start crash activity", e);
        }
        
        // 结束进程
        Process.killProcess(Process.myPid());
        System.exit(1);
    }
    
    private String getCrashInfo(Thread thread, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        // 添加线程信息
        pw.append("Thread: ").append(thread.getName()).append("\n");
        pw.append("Thread ID: ").append(String.valueOf(thread.getId())).append("\n\n");
        
        // 添加异常堆栈
        throwable.printStackTrace(pw);
        
        // 添加设备信息
        pw.append("\n\n--- Device Info ---\n");
        pw.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append("\n");
        pw.append("Device: ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append("\n");
        pw.append("SDK: ").append(String.valueOf(android.os.Build.VERSION.SDK_INT)).append("\n");
        
        return sw.toString();
    }
}