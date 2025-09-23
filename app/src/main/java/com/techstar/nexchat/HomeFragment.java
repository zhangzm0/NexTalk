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
    private List<Object> items; // 包含菜单项和聊天历史

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
		// 添加菜单项
		items.add(new HomeMenuItem(android.R.drawable.ic_menu_add, "新建对话", new View.OnClickListener() {
						  @Override
						  public void onClick(View v) {
							  createNewChat();
						  }
					  }));

		items.add(new HomeMenuItem(android.R.drawable.ic_menu_manage, "API供应商管理", new View.OnClickListener() {
						  @Override
						  public void onClick(View v) {
							  openApiProviders();
						  }
					  }));

		items.add(new HomeMenuItem(android.R.drawable.ic_menu_preferences, "设置", new View.OnClickListener() {
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

	private void loadChatHistory() {
		// 从数据库加载聊天历史，这里先添加示例数据
		ChatHistory chat1 = new ChatHistory("技术问题讨论", "如何优化Android应用性能？");
		chat1.setMessageCount(5);
		chat1.setTimestamp(System.currentTimeMillis() - 30 * 60 * 1000); // 30分钟前

		ChatHistory chat2 = new ChatHistory("学习计划", "本周学习安排");
		chat2.setMessageCount(3);
		chat2.setTimestamp(System.currentTimeMillis() - 2 * 60 * 60 * 1000); // 2小时前

		ChatHistory chat3 = new ChatHistory("项目讨论", "新功能需求分析");
		chat3.setMessageCount(8);
		chat3.setTimestamp(System.currentTimeMillis() - 24 * 60 * 60 * 1000); // 1天前

		items.add(chat1);
		items.add(chat2);
		items.add(chat3);
	}

    private void createNewChat() {
        // 实现新建对话逻辑
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToChatPage();
        }
    }

    private void openApiProviders() {
        Intent intent = new Intent(getActivity(), ApiProvidersActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
    }
}
