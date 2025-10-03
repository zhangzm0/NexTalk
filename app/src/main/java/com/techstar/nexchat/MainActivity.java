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
import android.view.ViewTreeObserver;

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

        
		// 设置布局回调监听
		getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(
			new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					AppLogger.d("MainActivity", "全局布局完成");
					// 移除监听，避免重复调用
					getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			});

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


    // 在 MainActivity.java 的 initViewPager 方法中添加详细日志
	private void initViewPager() {
		viewPager = findViewById(R.id.viewPager);
		AppLogger.d("MainActivity", "初始化ViewPager");

		pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
			@Override
			public int getCount() {
				return 3;
			}

			@Override
			public Fragment getItem(int position) {
				AppLogger.d("MainActivity", "创建Fragment位置: " + position);
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

			// 添加这个方法，确保Fragment不会重复创建
			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				AppLogger.d("MainActivity", "instantiateItem位置: " + position);
				return super.instantiateItem(container, position);
			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object) {
				AppLogger.d("MainActivity", "destroyItem位置: " + position);
				super.destroyItem(container, position, object);
			}
		};

		viewPager.setAdapter(pagerAdapter);
		viewPager.setCurrentItem(1); // 默认显示聊天页面
		AppLogger.d("MainActivity", "ViewPager设置完成，默认页面: 1");

		// 添加页面切换监听器
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
				@Override
				public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

				@Override
				public void onPageSelected(int position) {
					String pageName;
					switch (position) {
						case 0: pageName = "主页"; break;
						case 1: pageName = "聊天"; break;
						case 2: pageName = "输入"; break;
						default: pageName = "未知";
					}
					AppLogger.d("MainActivity", "=== 切换到页面: " + position + " (" + pageName + ") ===");

					// 当切换到聊天页面时，记录状态但不强制刷新
					if (position == 1 && chatFragment != null) {
						AppLogger.d("MainActivity", "聊天页面已激活");
					}
				}

				@Override
				public void onPageScrollStateChanged(int state) {
					String stateName;
					switch (state) {
						case ViewPager.SCROLL_STATE_IDLE: 
							stateName = "空闲"; 
							break;
						case ViewPager.SCROLL_STATE_DRAGGING: 
							stateName = "拖动"; 
							break;
						case ViewPager.SCROLL_STATE_SETTLING: 
							stateName = "惯性滚动"; 
							break;
						default: stateName = "未知";
					}
					AppLogger.d("MainActivity", "ViewPager滚动状态: " + stateName);
				}
			});
	}

    // 在 MainActivity 中添加页面切换延迟
	// 在 MainActivity.java 中修改
	public void switchToChatPage() {
		if (viewPager != null) {
			AppLogger.d("MainActivity", "switchToChatPage - 开始切换");
			viewPager.setCurrentItem(1);
			AppLogger.d("MainActivity", "switchToChatPage - 切换完成");

			// 完全移除任何刷新调用，避免触发滚动
			// 不调用 chatFragment.refreshConversation()
		}
	}


}
