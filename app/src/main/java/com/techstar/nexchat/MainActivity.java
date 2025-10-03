package com.techstar.nexchat;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.techstar.nexchat.adapter.ViewPagerAdapter;
import com.techstar.nexchat.util.CrashHandler;
import com.techstar.nexchat.util.FileLogger;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private FileLogger logger;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        logger = FileLogger.getInstance(this);
        logger.i(TAG, "MainActivity created");
        
        // 初始化全局异常处理
        CrashHandler.getInstance().init(this);
        
        initViewPager();
    }
    
    private void initViewPager() {
        viewPager = findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        
        // 设置默认页面为聊天页（中间页）
        viewPager.setCurrentItem(1, false);
        
        // 添加页面切换监听
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                logger.d(TAG, "Page changed to: " + position);
            }
        });
    }
    
    public void switchToChatPage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1, true);
        }
    }
    
    public void switchToInputPage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(2, true);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        logger.i(TAG, "MainActivity destroyed");
    }
}