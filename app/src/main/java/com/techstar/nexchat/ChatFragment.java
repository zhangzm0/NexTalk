package com.techstar.nexchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.techstar.nexchat.data.ChatRepo;
import com.techstar.nexchat.model.Message;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;

public class ChatFragment extends Fragment {

    private ListView listView;
    private MessageAdapter adapter;
    private Markwon markwon;

    /* 接收流式 delta */
    private final BroadcastReceiver streamReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String delta = intent.getStringExtra("delta");
            if (delta == null) return;
            /* 替换最后一条 assistant 消息 */
            if (adapter.getCount() > 0) {
                Message last = adapter.getItem(adapter.getCount() - 1);
                if ("assistant".equals(last.role)) {
                    last.content = delta;
                    adapter.notifyDataSetChanged();
                    listView.setSelection(adapter.getCount() - 1);
                }
            }
        }
    };

    /* 接收普通刷新 */
    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frag_chat, container, false);
        listView = (ListView) root.findViewById(R.id.chat_list);

        /* 构建 markwon */
        markwon = Markwon.builder(getContext())
			.usePlugin(JLatexMathPlugin.create(18f)) // 18sp 公式字号
			.build();

        adapter = new MessageAdapter(getContext(), markwon);
        listView.setAdapter(adapter);
        refresh();

        /* 注册广播 */
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
        lbm.registerReceiver(refreshReceiver, new IntentFilter("chat_changed"));
        lbm.registerReceiver(streamReceiver, new IntentFilter("stream_delta"));
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(refreshReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(streamReceiver);
    }

    /* 从数据库拉当前会话全部消息 */
    private void refresh() {
        List<Message> data = ChatRepo.get(getContext()).getCurrentMessages();
        adapter.replace(data);
        listView.setSelection(data.size() - 1);
    }
}

