package com.techstar.nexchat;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.techstar.nexchat.model.ApiProvider;
import java.util.ArrayList;
import java.util.List;

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

	

	private String getSelectedProviderId() {
		// 实现获取当前选择的供应商ID
		return "default_provider";
	}
	
	// ... 其他代码不变

	private String currentProviderId = "";
	private String currentModel = "";

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

		// 模型选择点击事件
		spinnerModel.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, android.view.MotionEvent event) {
					if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
						showModelSelector();
						return true;
					}
					return false;
				}
			});

		// 初始加载模型
		loadAvailableModels();
	}

	private void showModelSelector() {
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
		View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_model_selector, null);
		builder.setView(dialogView);

		final android.app.AlertDialog dialog = builder.create();
		dialog.show();

		setupModelSelector(dialogView, dialog);
	}

	private void setupModelSelector(View dialogView, final android.app.AlertDialog dialog) {
		final Spinner spinnerProviders = dialogView.findViewById(R.id.spinnerProviders);
		final ListView listViewModels = dialogView.findViewById(R.id.listViewModels);
		Button btnCancel = dialogView.findViewById(R.id.btnCancel);
		Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

		// 加载供应商列表
		final List<ApiProvider> providers = loadProviders();
		ArrayAdapter<ApiProvider> providerAdapter = new ArrayAdapter<ApiProvider>(getActivity(), 
																				  android.R.layout.simple_spinner_item, providers) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView textView = (TextView) super.getView(position, convertView, parent);
				textView.setTextColor(0xFFFFFFFF);
				textView.setText(getItem(position).getName());
				return textView;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
				textView.setTextColor(0xFFFFFFFF);
				textView.setText(getItem(position).getName());
				textView.setBackgroundColor(0xFF2D2D2D);
				return textView;
			}
		};
		spinnerProviders.setAdapter(providerAdapter);

		// 供应商选择监听
		spinnerProviders.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
					ApiProvider selectedProvider = providers.get(position);
					updateModelsList(listViewModels, selectedProvider);
				}

				@Override
				public void onNothingSelected(android.widget.AdapterView<?> parent) {}
			});

		// 模型选择监听
		final String[] selectedModel = {""};
		listViewModels.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
					selectedModel[0] = (String) parent.getItemAtPosition(position);
					// 高亮显示选中的模型
					for (int i = 0; i < parent.getChildCount(); i++) {
						parent.getChildAt(i).setBackgroundColor(0x002D2D2D);
					}
					view.setBackgroundColor(0xFF2196F3);
				}
			});

		btnCancel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});

		btnConfirm.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!selectedModel[0].isEmpty()) {
						ApiProvider selectedProvider = (ApiProvider) spinnerProviders.getSelectedItem();
						currentProviderId = selectedProvider.getId();
						currentModel = selectedModel[0];
						updateModelSpinner();
						dialog.dismiss();
					} else {
						Toast.makeText(getActivity(), "请选择一个模型", Toast.LENGTH_SHORT).show();
					}
				}
			});

		// 设置默认选择
		if (!currentProviderId.isEmpty()) {
			for (int i = 0; i < providers.size(); i++) {
				if (providers.get(i).getId().equals(currentProviderId)) {
					spinnerProviders.setSelection(i);
					break;
				}
			}
		}
	}

	private void updateModelsList(ListView listView, ApiProvider provider) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), 
																android.R.layout.simple_list_item_1, provider.getModels()) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView textView = (TextView) super.getView(position, convertView, parent);
				textView.setTextColor(0xFFFFFFFF);
				textView.setPadding(16, 12, 16, 12);
				return textView;
			}
		};
		listView.setAdapter(adapter);
	}

	private void updateModelSpinner() {
		// 更新主界面的模型显示
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), 
																android.R.layout.simple_spinner_item, new String[]{currentModel}) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView textView = (TextView) super.getView(position, convertView, parent);
				textView.setTextColor(0xFFFFFFFF);
				textView.setText(getItem(position));
				return textView;
			}
		};
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerModel.setAdapter(adapter);
	}

	private List<ApiProvider> loadProviders() {
		// 从SharedPreferences加载供应商列表
		List<ApiProvider> providers = new ArrayList<>();
		// 这里实现实际的加载逻辑
		return providers;
	}

	private void loadAvailableModels() {
		// 初始加载默认模型
		if (currentModel.isEmpty()) {
			currentModel = "gpt-3.5-turbo";
			updateModelSpinner();
		}
	}

	private void sendMessage() {
		String message = etMessage.getText().toString().trim();
		if (!message.isEmpty()) {
			if (currentProviderId.isEmpty() || currentModel.isEmpty()) {
				Toast.makeText(getActivity(), "请先选择模型", Toast.LENGTH_SHORT).show();
				return;
			}

			// 发送消息到ChatFragment
			if (getActivity() instanceof MainActivity) {
				MainActivity mainActivity = (MainActivity) getActivity();
				mainActivity.sendChatMessage(message, currentProviderId, currentModel);
			}

			etMessage.setText("");
		}
	}
}
