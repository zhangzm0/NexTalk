package com.techstar.nexchat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.techstar.nexchat.R;

public class MainActivity extends AppCompatActivity {

    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pager);
        // 禁止预加载，小屏省内存
        pager.setOffscreenPageLimit(0);
        pager.setAdapter(new SimplePagerAdapter(getSupportFragmentManager()));
        // 默认停在中间页：聊天页
        pager.setCurrentItem(1, false);
    }

    /* 暴露给 Fragment 跳页 */
    public void setPage(int idx) {
        if (pager != null) pager.setCurrentItem(idx, true);
    }

    private static class SimplePagerAdapter extends FragmentPagerAdapter {
        SimplePagerAdapter(FragmentManager fm) { super(fm); }

        @Override
        public int getCount() { return 3; }

        @Override
        public Fragment getItem(int pos) {
            switch (pos) {
                case 0: return HomeFragment.newInstance();
                case 1: return ChatFragment.newInstance();
                case 2: return InputFragment.newInstance();
                default: return ChatFragment.newInstance();
            }
        }
    }
}

