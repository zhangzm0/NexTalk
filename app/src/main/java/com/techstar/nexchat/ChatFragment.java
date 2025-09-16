package com.techstar.nexchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.techstar.nexchat.R;
import com.techstar.nexchat.data.ChatRepo;
import com.techstar.nexchat.model.Message;

import java.util.List;

public class ChatFragment extends Fragment {

    private ListView listView;
    private MessageAdapter adapter;

    /* 统一刷新广播，InputFragment 发，ChatFragment 收 */
    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frag_chat, container, false);
        listView = root.findViewById(R.id.chat_list);
        adapter = new MessageAdapter(getContext());
        listView.setAdapter(adapter);
        refresh();                 // 第一次加载
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext())
			.registerReceiver(refreshReceiver,
							  new IntentFilter("chat_changed"));
        refresh();                 // 切回来也刷一次
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext())
			.unregisterReceiver(refreshReceiver);
    }

    /* 公开方法，供外部立即刷新 */
    public void refresh() {
        List<Message> data = ChatRepo.get(getContext()).getAll();
        adapter.replace(data);
        listView.setSelection(data.size() - 1); // 自动滚到底
    }
}

