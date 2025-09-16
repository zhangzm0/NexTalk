package com.techstar.nexchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.techstar.nexchat.data.ChatRepo;

import java.util.List;

public class HomeFragment extends Fragment {

    private ListView listView;
    private ArrayAdapter<String> adapter;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.frag_home, container, false);

        /* 会话列表 */
        listView = (ListView) root.findViewById(R.id.list_history);
        refreshSessionList();          // 第一次加载

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					/* 拿到 session id */
					long sessionId = getSessionIdByPosition(position);
					ChatRepo.get(getContext()).switchSession(sessionId);

					/* 通知 ChatFragment 刷新 */
					LocalBroadcastManager.getInstance(getContext())
                        .sendBroadcast(new Intent("chat_changed"));

					/* 跳到聊天页 */
					((MainActivity) getActivity()).setPage(1);
				}
			});

        /* 新建对话按钮 */
        Button btnNew = (Button) root.findViewById(R.id.btn_new);
        btnNew.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					/* 创建并自动切换新会话 */
					ChatRepo.get(getContext()).createSession("新对话");
					LocalBroadcastManager.getInstance(getContext())
                        .sendBroadcast(new Intent("chat_changed"));
					((MainActivity) getActivity()).setPage(1);
				}
			});

        /* 设置按钮 */
        Button btnSettings = (Button) root.findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(getContext(), SettingsActivity.class));
				}
			});

        return root;
    }

    /* 把会话标题重新倒进 ListView */
    private void refreshSessionList() {
        List<String> titles = ChatRepo.get(getContext()).getSessionTitles();
        if (adapter == null) {
            adapter = new ArrayAdapter<String>(
				getContext(),
				android.R.layout.simple_list_item_1,
				titles);
            listView.setAdapter(adapter);
        } else {
            adapter.clear();
            adapter.addAll(titles);
            adapter.notifyDataSetChanged();
        }
    }

    /* 根据 ListView position 取 session id（简易版） */
    private long getSessionIdByPosition(int position) {
        /* 跟 getSessionTitles() 顺序一致 */
        return ChatRepo.get(getContext()).getSessions().get(position).id;
    }

    /* 从别的页回来可能新建了会话，刷新一下 */
    @Override
    public void onResume() {
        super.onResume();
        refreshSessionList();
    }
}

