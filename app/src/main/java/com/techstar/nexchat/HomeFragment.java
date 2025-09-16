package com.techstar.nexchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

import com.techstar.nexchat.data.ChatRepo;

public class HomeFragment extends Fragment {

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.frag_home, container, false);

        // 新建对话
        Button btnNew = (Button) root.findViewById(R.id.btn_new);
        btnNew.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 清空历史
					ChatRepo.get(getContext()).clear();
					// 跳到聊天页
					((MainActivity) getActivity()).setPage(1);
				}
			});

        // 历史列表
        ListView listHistory = (ListView) root.findViewById(R.id.list_history);
        final List<String> titles = ChatRepo.get(getContext()).getSessionTitles(); // 自己写个方法
        listHistory.setAdapter(new ArrayAdapter<String>(
								   getContext(),
								   android.R.layout.simple_list_item_1,
								   titles));
        listHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					// 恢复选中会话即可，这里直接跳聊天页
					((MainActivity) getActivity()).setPage(1);
				}
			});

        // 设置按钮
        Button btnSettings = (Button) root.findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(getContext(), SettingsActivity.class));
				}
			});

        return root;
    }
}

