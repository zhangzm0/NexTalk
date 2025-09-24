package com.techstar.nexchat;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
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


	@Override
	public void onResume() {
		super.onResume();
		// 页面显示时自动获取焦点
		if (etMessage != null) {
			etMessage.requestFocus();

			// 延迟显示键盘，确保布局已经完成
			etMessage.postDelayed(new Runnable() {
					@Override
					public void run() {
						InputMethodManager imm = (InputMethodManager) getActivity()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
						if (imm != null) {
							imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
						}
					}
				}, 100);
		}
	}

	// 修改初始化方法
	private void initViews(View view) {
		etMessage = view.findViewById(R.id.etMessage);
		btnSend = view.findViewById(R.id.btnSend);
		btnUpload = view.findViewById(R.id.btnUpload);
		btnNetwork = view.findViewById(R.id.btnNetwork);
		spinnerModel = view.findViewById(R.id.spinnerModel);

		// 设置输入框焦点变化监听
		etMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(final View v, boolean hasFocus) {
					if (hasFocus) {
						// 输入框获得焦点时，确保内容可见
						v.post(new Runnable() {
								@Override
								public void run() {
									ScrollView scrollView = getActivity().findViewById(R.id.scrollView);
									if (scrollView != null) {
										scrollView.smoothScrollTo(0, v.getBottom());
									}
								}
							});
					}
				}
			});

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
	
	// ... 其他代码不变

	private void sendMessage() {
		String message = etMessage.getText().toString().trim();
		if (!message.isEmpty()) {
			// 获取当前选择的模型和供应商
			String selectedModel = (String) spinnerModel.getSelectedItem();
			String providerId = getSelectedProviderId(); // 需要实现这个方法

			// 发送消息到ChatFragment
			if (getActivity() instanceof MainActivity) {
				MainActivity mainActivity = (MainActivity) getActivity();
				mainActivity.sendChatMessage(message, providerId, selectedModel);
			}

			etMessage.setText("");
		}
	}

	private String getSelectedProviderId() {
		// 实现获取当前选择的供应商ID
		return "default_provider";
	}
}
