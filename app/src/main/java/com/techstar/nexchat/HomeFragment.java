package com.techstar.nexchat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.ArrayList;
import android.content.Intent;
import android.widget.Toast;

public class HomeFragment extends Fragment {

    private Button btnNewChat, btnSettings;
    private ListView chatHistoryList;
    private ArrayAdapter<String> historyAdapter;
    private ArrayList<String> chatHistory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupClickListeners();
        loadChatHistory();

        return view;
    }

    private void initViews(View view) {
        btnNewChat = (Button) view.findViewById(R.id.btnNewChat);
        btnSettings = (Button) view.findViewById(R.id.btnSettings);
        chatHistoryList = (ListView) view.findViewById(R.id.chatHistoryList);

        chatHistory = new ArrayList<String>();
        historyAdapter = new ArrayAdapter<String>(getActivity(), 
												  android.R.layout.simple_list_item_1, chatHistory);
        chatHistoryList.setAdapter(historyAdapter);
    }

    private void setupClickListeners() {
        btnNewChat.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 新建对话逻辑
					createNewChat();
				}
			});

        btnSettings.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 打开设置
					openSettings();
				}
			});

        chatHistoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					// 加载选中的聊天记录
					loadChat(position);
				}
			});
    }

    private void createNewChat() {
        // 实现新建对话逻辑
    }

    private void openSettings() {
		try {
			Intent intent = new Intent(getActivity(), SettingsActivity.class);
			startActivity(intent);
		} catch (Exception e) {
			Toast.makeText(getActivity(), "打开设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

    private void loadChat(int position) {
        // 实现加载聊天记录逻辑
    }

    private void loadChatHistory() {
        // 从数据库加载聊天历史
        chatHistory.add("示例对话 1");
        chatHistory.add("示例对话 2");
        historyAdapter.notifyDataSetChanged();
    }
}
