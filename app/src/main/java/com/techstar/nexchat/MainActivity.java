package com.techstar.nexchat;

import android.os.Bundle;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.View;

public class MainActivity extends FragmentActivity {

    private ViewPager viewPager;
    private FragmentPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 先初始化异常捕获
        try {
            CrashHandler.getInstance().init(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 启用矢量图兼容
        androidx.appcompat.app.AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        setContentView(R.layout.activity_main);
        initViewPager();

        // 测试崩溃捕获是否工作（取消注释来测试）
        // testCrashHandler();
    }

    private void testCrashHandler() {
        // 测试按钮，点击会崩溃
        findViewById(R.id.viewPager).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 人为制造崩溃
					throw new RuntimeException("测试崩溃捕获功能");
				}
			});
    }

    

    public void switchToChatPage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1); // 切换到聊天页面
        }
    }



	


	private ChatFragment chatFragment;

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
						return new InputFragment();
					default:
						return null;
				}
			}
		};

		viewPager.setAdapter(pagerAdapter);
		viewPager.setCurrentItem(1); // 默认显示聊天页面
	}

	public void sendChatMessage(String message, String providerId, String model) {
		if (chatFragment != null) {
			chatFragment.sendMessage(message, providerId, model);
		} else {
			Toast.makeText(this, "聊天页面未初始化", Toast.LENGTH_SHORT).show();
		}
	}
}
