package com.techstar.nexchat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends FragmentActivity {

    private ViewPager viewPager;
    public  FragmentPagerAdapter pagerAdapter;
    public  ChatFragment chatFragment;
    public  InputFragment inputFragment;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化异常捕获
        CrashHandler.getInstance().init(this);

        // 初始化全局日志记录器
        AppLogger.getInstance().init(this);



        // 使用新的日志方法
        AppLogger.i("MainActivity", "=================App started===============");

        setContentView(R.layout.activity_main);
        initViewPager();
    }

    public void sendChatMessage(final String message, final String providerId, final String model) {
        if (chatFragment != null && chatFragment.isAdded()) {
            chatFragment.sendMessage(message, providerId, model);
        } else {
            // 如果聊天页面未初始化，尝试重新初始化
            Toast.makeText(this, "正在初始化聊天页面...", Toast.LENGTH_SHORT).show();
            if (chatFragment == null) {
                chatFragment = new ChatFragment();
            }
            // 延迟发送消息
            new android.os.Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						if (chatFragment != null) {
							chatFragment.sendMessage(message, providerId, model);
							// 切换到聊天页面查看回复
							viewPager.setCurrentItem(1);
						}
					}
				}, 500);
        }
    }

    private Fragment createFallbackFragment() {
        return new Fragment() {
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                TextView textView = new TextView(getActivity());
                textView.setText("页面加载中...");
                textView.setTextColor(0xFFFFFFFF);
                textView.setBackgroundColor(0xFF121212);
                textView.setGravity(android.view.Gravity.CENTER);
                return textView;
            }
        };
    }
// ... 其他代码不变

    private void initViewPager() {
        viewPager = findViewById(R.id.viewPager);

        pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return new HomeFragment();
                    case 1:
                        chatFragment = new ChatFragment();
                        return chatFragment;
                    case 2:
                        inputFragment = new InputFragment();
                        return inputFragment;
                    default:
                        return new Fragment();
                }
            }
        };

        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(1); // 默认显示聊天页面

        // 添加页面切换监听
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
				@Override
				public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

				@Override
				public void onPageSelected(int position) {
					AppLogger.d("MainActivity", "切换到页面: " + position);

					// 当切换到聊天页面时，刷新对话显示
					if (position == 1 && chatFragment != null) {
						AppLogger.d("MainActivity", "刷新聊天页面");
						chatFragment.refreshConversation();
					}
				}

				@Override
				public void onPageScrollStateChanged(int state) {}
			});
    }

    public void switchToChatPage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1);
            AppLogger.d("MainActivity", "切换到聊天页面");

            // 延迟刷新，确保页面切换完成
            new android.os.Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						if (chatFragment != null) {
							chatFragment.refreshConversation();
						}
					}
				}, 100);
        }
    }
}
