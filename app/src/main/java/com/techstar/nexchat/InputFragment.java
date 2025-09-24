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
import android.widget.LinearLayout;
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



	private String currentProviderId = "";
	private String currentModel = "";


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
    

    

    private void setDialogStyle(android.app.AlertDialog dialog) {
        // 延迟设置样式，确保对话框已创建
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
				@Override
				public void onShow(android.content.DialogInterface dialogInterface) {
					android.app.AlertDialog dialog = (android.app.AlertDialog) dialogInterface;

					// 设置背景色
					android.graphics.drawable.ColorDrawable background = new android.graphics.drawable.ColorDrawable(0xFF1E1E1E);
					dialog.getWindow().setBackgroundDrawable(background);

					// 设置列表颜色
					android.widget.ListView listView = dialog.getListView();
					if (listView != null) {
						listView.setBackgroundColor(0xFF1E1E1E);
						listView.setDivider(new android.graphics.drawable.ColorDrawable(0xFF333333));
						listView.setDividerHeight(1);

						// 设置列表项颜色
						try {
							java.lang.reflect.Field field = android.widget.AbsListView.class.getDeclaredField("mSelector");
							field.setAccessible(true);
							android.graphics.drawable.Drawable selector = (android.graphics.drawable.Drawable) field.get(listView);
							if (selector != null) {
								selector.setColorFilter(0xFF2196F3, android.graphics.PorterDuff.Mode.SRC_ATOP);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// 设置按钮颜色
					dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFFFFFFF);
					dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(0xFF2D2D2D);
				}
			});
    }


    // 简化供应商加载（添加示例数据用于测试）
    private List<ApiProvider> loadProviders() {
        List<ApiProvider> providers = new ArrayList<>();

        // 从SharedPreferences加载实际数据
        try {
            String providerIds = getActivity().getSharedPreferences("api_providers", android.content.Context.MODE_PRIVATE)
                .getString("provider_ids", "");

            if (!providerIds.isEmpty()) {
                String[] ids = providerIds.split(",");
                for (String id : ids) {
                    ApiProvider provider = loadProviderById(id);
                    if (provider != null && !provider.getModels().isEmpty()) {
                        providers.add(provider);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 如果没有数据，添加示例数据用于测试
        if (providers.isEmpty()) {
            ApiProvider exampleProvider = new ApiProvider("OpenAI", "https://api.openai.com/v1", "sk-...");
            exampleProvider.getModels().add("gpt-3.5-turbo");
            exampleProvider.getModels().add("gpt-4");
            exampleProvider.setId("example_1");
            providers.add(exampleProvider);
        }

        return providers;
    }

    private ApiProvider loadProviderById(String providerId) {
        // 从SharedPreferences加载供应商数据
        try {
            String name = getActivity().getSharedPreferences("api_providers", android.content.Context.MODE_PRIVATE)
                .getString(providerId + "_name", "");
            String url = getActivity().getSharedPreferences("api_providers", android.content.Context.MODE_PRIVATE)
                .getString(providerId + "_url", "");
            String key = getActivity().getSharedPreferences("api_providers", android.content.Context.MODE_PRIVATE)
                .getString(providerId + "_key", "");
            int modelCount = getActivity().getSharedPreferences("api_providers", android.content.Context.MODE_PRIVATE)
                .getInt(providerId + "_model_count", 0);

            if (!name.isEmpty()) {
                ApiProvider provider = new ApiProvider(name, url, key);
                provider.setId(providerId);

                // 加载模型列表
                for (int i = 0; i < modelCount; i++) {
                    String model = getActivity().getSharedPreferences("api_providers", android.content.Context.MODE_PRIVATE)
                        .getString(providerId + "_model_" + i, "");
                    if (!model.isEmpty()) {
                        provider.getModels().add(model);
                    }
                }

                return provider;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


	// ... 其他代码不变

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

		// 修复：移除错误的setOnClickListener，改用触摸监听
		spinnerModel.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, android.view.MotionEvent event) {
					if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
						showModelSelector();
						return true; // 消费事件，防止Spinner默认行为
					}
					return false;
				}
			});

		// 也添加点击监听到Spinner的文本部分（如果有）
		try {
			// 获取Spinner内部的TextView
			java.lang.reflect.Field field = android.widget.AbsSpinner.class.getDeclaredField("mPopup");
			field.setAccessible(true);
			Object popup = field.get(spinnerModel);

			if (popup instanceof android.widget.ListPopupWindow) {
				android.widget.ListPopupWindow listPopup = (android.widget.ListPopupWindow) popup;
				View anchor = listPopup.getAnchorView();
				if (anchor != null) {
					anchor.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								showModelSelector();
							}
						});
				}
			}
		} catch (Exception e) {
			// 如果反射失败，使用备用方案：在Spinner旁边添加一个选择按钮
			addModelSelectButton();
		}

		// 初始加载模型
		loadAvailableModels();
	}

	// 备用方案：在Spinner旁边添加选择按钮
	private void addModelSelectButton() {
		// 创建一个包含Spinner和按钮的布局
		LinearLayout container = new LinearLayout(getActivity());
		container.setOrientation(LinearLayout.HORIZONTAL);
		container.setLayoutParams(new LinearLayout.LayoutParams(
									  LinearLayout.LayoutParams.MATCH_PARENT,
									  LinearLayout.LayoutParams.WRAP_CONTENT
								  ));

		// 移除原来的Spinner从父布局
		ViewGroup parent = (ViewGroup) spinnerModel.getParent();
		int index = parent.indexOfChild(spinnerModel);
		parent.removeView(spinnerModel);

		// 设置Spinner权重
		LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
			0, LinearLayout.LayoutParams.WRAP_CONTENT
		);
		spinnerParams.weight = 1;
		spinnerModel.setLayoutParams(spinnerParams);

		// 添加选择按钮
		ImageButton selectButton = new ImageButton(getActivity());
		selectButton.setImageResource(android.R.drawable.ic_menu_more);
		selectButton.setBackgroundColor(0x001E1E1E);
		selectButton.setColorFilter(0xFFFFFFFF);
		selectButton.setLayoutParams(new LinearLayout.LayoutParams(
										 LinearLayout.LayoutParams.WRAP_CONTENT,
										 LinearLayout.LayoutParams.WRAP_CONTENT
									 ));
		selectButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showModelSelector();
				}
			});

		// 添加到容器
		container.addView(spinnerModel);
		container.addView(selectButton);

		// 添加到原来的位置
		parent.addView(container, index);
	}

	// 简化模型选择器显示
	private void showModelSelector() {
		// 延迟显示，避免触摸事件冲突
		spinnerModel.post(new Runnable() {
				@Override
				public void run() {
					showProviderSelector();
				}
			});
	}

	// 修复：确保对话框在UI线程显示
	private void showProviderSelector() {
		if (getActivity() == null) return;

		getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final List<ApiProvider> providers = loadProviders();
					if (providers.isEmpty()) {
						android.widget.Toast.makeText(getActivity(), "请先添加API供应商", android.widget.Toast.LENGTH_SHORT).show();
						return;
					}

					final String[] providerNames = new String[providers.size()];
					for (int i = 0; i < providers.size(); i++) {
						providerNames[i] = providers.get(i).getName() + " (" + providers.get(i).getModels().size() + "个模型)";
					}

					android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
					builder.setTitle("选择供应商")
						.setItems(providerNames, new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(android.content.DialogInterface dialog, int which) {
								showModelSelectorForProvider(providers.get(which));
							}
						})
						.setNegativeButton("取消", null);

					android.app.AlertDialog dialog = builder.create();
					dialog.show();
				}
			});
	}

	// 修复模型选择显示
	private void showModelSelectorForProvider(final ApiProvider provider) {
		if (getActivity() == null) return;

		getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (provider.getModels().isEmpty()) {
						android.widget.Toast.makeText(getActivity(), "该供应商没有可用模型", android.widget.Toast.LENGTH_SHORT).show();
						return;
					}

					final String[] models = provider.getModels().toArray(new String[0]);

					android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
					builder.setTitle("选择模型 - " + provider.getName())
						.setItems(models, new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(android.content.DialogInterface dialog, int which) {
								currentProviderId = provider.getId();
								currentModel = models[which];
								updateModelSpinner();
								android.widget.Toast.makeText(getActivity(), "已选择: " + currentModel, android.widget.Toast.LENGTH_SHORT).show();
							}
						})
						.setNegativeButton("取消", null);

					android.app.AlertDialog dialog = builder.create();
					dialog.show();
				}
			});
	}

	// 修复模型显示更新
	private void updateModelSpinner() {
		if (getActivity() == null || spinnerModel == null) return;

		getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String displayText = currentModel;
					if (displayText.length() > 15) {
						displayText = displayText.substring(0, 15) + "...";
					}

					ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), 
																			android.R.layout.simple_spinner_item, new String[]{displayText}) {
						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							TextView textView = (TextView) super.getView(position, convertView, parent);
							textView.setTextColor(0xFFFFFFFF);
							textView.setText(getItem(position));
							textView.setSingleLine(true);
							return textView;
						}
					};
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					spinnerModel.setAdapter(adapter);
				}
			});
	}

	// ... 其他方法保持不变
}
