package com.techstar.nexchat.model;

import android.view.View;

public class HomeMenuItem {
    private int iconRes;
    private String title;
    private View.OnClickListener onClickListener;

    public HomeMenuItem(int iconRes, String title, View.OnClickListener onClickListener) {
        this.iconRes = iconRes;
        this.title = title;
        this.onClickListener = onClickListener;
    }

    public int getIconRes() { return iconRes; }
    public String getTitle() { return title; }
    public View.OnClickListener getOnClickListener() { return onClickListener; }
}
