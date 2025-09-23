package com.techstar.nexchat;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.WindowManager;

public class MainActivity extends FragmentActivity {

    private ViewPager viewPager;
    private FragmentPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

		// 在MainActivity的onCreate中添加
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViewPager();
    }

    private void initViewPager() {
        viewPager = (ViewPager) findViewById(R.id.viewPager);

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
                        return new ChatFragment();
                    case 2:
                        return new InputFragment();
                    default:
                        return null;
                }
            }
        };

        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(1);
    }


	public void switchToChatPage() {
		if (viewPager != null) {
			viewPager.setCurrentItem(1); // 切换到聊天页面
		}
	}
	
}
