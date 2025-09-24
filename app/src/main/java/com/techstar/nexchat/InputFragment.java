package com.techstar.nexchat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public class InputFragment extends Fragment {

    private EditText etMessage;
    private ImageButton btnSend, btnUpload, btnNetwork;
    private Spinner spinnerModel;
    private ArrayAdapter<String> modelAdapter;
    private ArrayList<String> modelList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container, false);

        initViews(view);
        setupClickListeners();
        loadModels();

        return view;
    }

    private void initViews(View view) {
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnUpload = view.findViewById(R.id.btnUpload);
        btnNetwork = view.findViewById(R.id.btnNetwork); // 修正这里
        spinnerModel = view.findViewById(R.id.spinnerModel);

        modelList = new ArrayList<String>();
        modelAdapter = new ArrayAdapter<String>(getActivity(), 
												android.R.layout.simple_spinner_item, modelList);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					sendMessage();
				}
			});

        btnUpload.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					uploadFile();
				}
			});

        btnNetwork.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleNetworkSearch();
				}
			});
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            // 实现发送消息逻辑
            etMessage.setText("");

            // 更新聊天页面
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                // 这里需要实现消息发送和接收的逻辑
            }
        }
    }

    private void uploadFile() {
        // 实现文件上传逻辑
        // 这里可以打开文件选择器
    }

    private void toggleNetworkSearch() {
        // 切换联网搜索状态
        boolean isEnabled = btnNetwork.getTag() == null || !(boolean) btnNetwork.getTag();
        btnNetwork.setTag(isEnabled);

        if (isEnabled) {
            btnNetwork.setColorFilter(0xFF4CAF50); // 绿色表示启用
        } else {
            btnNetwork.setColorFilter(0xFF666666); // 灰色表示禁用
        }
    }

    private void loadModels() {
        // 从设置加载模型列表
        modelList.add("GPT-3.5");
        modelList.add("GPT-4");
        modelAdapter.notifyDataSetChanged();
    }
}
