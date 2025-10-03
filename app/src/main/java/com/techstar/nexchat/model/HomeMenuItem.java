package com.techstar.nexchat.model;

public class HomeMenuItem {
    private String title;
    private int iconResId;
    private MenuItemClickListener clickListener;
    
    public interface MenuItemClickListener {
        void onClick();
    }
    
    public HomeMenuItem(String title, int iconResId, MenuItemClickListener clickListener) {
        this.title = title;
        this.iconResId = iconResId;
        this.clickListener = clickListener;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getIconResId() {
        return iconResId;
    }
    
    public MenuItemClickListener getClickListener() {
        return clickListener;
    }
}