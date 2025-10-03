package com.techstar.nexchat.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.techstar.nexchat.fragment.HomeFragment;
import com.techstar.nexchat.fragment.ChatFragment;
import com.techstar.nexchat.fragment.InputFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {
    private static final int PAGE_COUNT = 3;
    
    public ViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }
    
    @NonNull
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
                return new ChatFragment();
        }
    }
    
    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}