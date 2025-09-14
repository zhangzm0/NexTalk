package com.techstar.nexchat;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // 黑色导航栏
        getWindow().setNavigationBarColor(0xff121212);

        ViewPager vp = new ViewPager(this);
        vp.setId(R.id.pager);
        vp.setAdapter(new NexPager(getSupportFragmentManager()));
        vp.setCurrentItem(1, false);          // 默认聊天页
        // 轻量切换动画（8 dp 视差 + 10 % 淡入）
        vp.setPageTransformer(false, new ViewPager.PageTransformer() {
                public void transformPage(View v, float pos) {
                    v.setTranslationX(pos < 0 ? 0 : -pos * 8);
                    v.setAlpha(1 - Math.abs(pos) * 0.1f);
                }
            });
        setContentView(vp);
    }

    private static class NexPager extends FragmentPagerAdapter {
        NexPager(FragmentManager fm) { super(fm); }
        @Override public int getCount() { return 3; }
        @Override public Fragment getItem(int p) {
            switch (p) {
                case 0: return new HomeFragment();
                case 1: return new ChatFragment();
                default: return new InputFragment();
            }
        }
    }


public void onUserInput(String text) { /* 发到 ViewModel / 网络 */ }
public void onChooseModel() { /* 弹出模型选择对话框 */ }
public void onNetToggle(boolean on) { /* 记录联网开关 */ }
public void onFilePicked(Uri uri) { /* 上传文件 */ }

// 供 HomeFragment 调用
public void openChat(long sessionId) {
    // TODO: 跳转到聊天页（后面再实现）
    Toast.makeText(this, "打开会话 ID=" + sessionId, Toast.LENGTH_SHORT).show();
}


}

