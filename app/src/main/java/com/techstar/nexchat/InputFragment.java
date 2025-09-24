package com.techstar.nexchat;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import java.util.ArrayList;

public class InputFragment extends Fragment {

    private EditText etMessage;
    private Button btnSend, btnUpload;
    private CheckBox cbNetwork;
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
        etMessage = (EditText) view.findViewById(R.id.etMessage);
        btnSend = (Button) view.findViewById(R.id.btnSend);
        btnUpload = (Button) view.findViewById(R.id.btnUpload);
        cbNetwork = (CheckBox) view.findViewById(R.id.btnNetwork);
        spinnerModel = (Spinner) view.findViewById(R.id.spinnerModel);

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
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            // 实现发送消息逻辑
            etMessage.setText("");
        }
    }

    private void uploadFile() {
        // 实现文件上传逻辑
    }

    private void loadModels() {
        // 从设置加载模型列表
        modelList.add("GPT-3.5");
        modelList.add("GPT-4");
        modelAdapter.notifyDataSetChanged();
    }
}
