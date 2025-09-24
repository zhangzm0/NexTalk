package com.techstar.nexchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.model.ChatHistory;
import com.techstar.nexchat.model.HomeMenuItem;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private HomeAdapter adapter;
    private List<Object> items;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initRecyclerView(view);
        loadData();

        return view;
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.chatHistoryList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        items = new ArrayList<>();
        adapter = new HomeAdapter(items);
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
    // 只保留必要的菜单项，并设置正确的图标
    items.add(new HomeMenuItem(R.drawable.ic_add_white_24dp, "新建对话", new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            createNewChat();
        }
    }));
    
    // 设置图标
    items.add(new HomeMenuItem(R.drawable.ic_settings_white_24dp, "设置", new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openSettings();
        }
    }));
    
    // 添加分隔符
    items.add("分隔符");
    
    // 添加聊天历史示例数据
    loadChatHistory();
    
    adapter.notifyDataSetChanged();
}

    private void createNewChat() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToChatPage();
        }
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(getActivity(), "打开设置失败: " + e.getMessage(), 
										  android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void loadChatHistory() {
        // 从数据库加载聊天历史，这里先添加示例数据
        ChatHistory chat1 = new ChatHistory("技术问题讨论", "如何优化Android应用性能？");
        chat1.setMessageCount(5);
        chat1.setTimestamp(System.currentTimeMillis() - 30 * 60 * 1000);

        ChatHistory chat2 = new ChatHistory("学习计划", "本周学习安排");
        chat2.setMessageCount(3);
        chat2.setTimestamp(System.currentTimeMillis() - 2 * 60 * 60 * 1000);

        items.add(chat1);
        items.add(chat2);
    }
}
