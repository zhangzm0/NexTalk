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
    private FragmentPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化异常捕获
        CrashHandler.getInstance().init(this);

        setContentView(R.layout.activity_main);
        initViewPager();
    }

    private void initViewPager() {
        viewPager = findViewById(R.id.viewPager);

        pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public Fragment getItem(int position) {
                // 简化Fragment创建，避免复杂初始化
                switch (position) {
                    case 0:
                        return new HomeFragment();
                    case 1:
                        return new ChatFragment();
                    case 2:
                        return new InputFragment();
                    default:
                        return new Fragment(); // 返回空Fragment作为备用
                }
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                // 添加错误处理
                try {
                    return super.instantiateItem(container, position);
                } catch (Exception e) {
                    // 如果Fragment初始化失败，返回一个简单的Fragment
                    Fragment fragment = new Fragment() {
                        @Override
                        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                            TextView textView = new TextView(getActivity());
                            textView.setText("Fragment加载失败");
                            textView.setTextColor(0xFFFFFFFF);
                            textView.setBackgroundColor(0xFF000000);
                            return textView;
                        }
                    };
                    return fragment;
                }
            }
        };

        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(1); // 默认显示聊天页面
    }





    public void switchToChatPage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1); // 切换到聊天页面
        }
    }
	
	private ChatFragment chatFragment;

	

	public void sendChatMessage(String message, String providerId, String model) {
		if (chatFragment != null) {
			chatFragment.sendMessage(message, providerId, model);
		} else {
			Toast.makeText(this, "聊天页面未初始化", Toast.LENGTH_SHORT).show();
		}
	}
}
