package com.techstar.nexchat.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.techstar.nexchat.fragment.HomeFragment;
import com.techstar.nexchat.fragment.ChatFragment;
import com.techstar.nexchat.fragment.InputFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private static final int PAGE_COUNT = 3;
    
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
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
    public int getItemCount() {
        return PAGE_COUNT;
    }
}