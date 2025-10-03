package com.techstar.nexchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.R;
import com.techstar.nexchat.model.HomeMenuItem;
import com.techstar.nexchat.util.FileLogger;
import java.util.List;

public class HomeMenuAdapter extends RecyclerView.Adapter<HomeMenuAdapter.ViewHolder> {
    private static final String TAG = "HomeMenuAdapter";
    
    private Context context;
    private List<HomeMenuItem> menuItems;
    private FileLogger logger;
    
    public HomeMenuAdapter(Context context, List<HomeMenuItem> menuItems) {
        this.context = context;
        this.menuItems = menuItems;
        this.logger = FileLogger.getInstance(context);
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_menu, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeMenuItem menuItem = menuItems.get(position);
        
        holder.icon.setImageResource(menuItem.getIconResId());
        holder.title.setText(menuItem.getTitle());
        
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuItem.getClickListener() != null) {
                    menuItem.getClickListener().onClick();
                }
                logger.d(TAG, "Menu item clicked: " + menuItem.getTitle());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return menuItems.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
        }
    }
}