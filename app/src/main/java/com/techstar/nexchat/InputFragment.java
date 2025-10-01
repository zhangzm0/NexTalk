package com.techstar.nexchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.techstar.nexchat.model.ApiProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InputFragment extends Fragment {

    private EditText etMessage;
    private ImageButton btnSend, btnUpload, btnNetwork;
    private Spinner spinnerModel;
    private ArrayAdapter<String> modelAdapter;
    private ArrayList<String> modelList;

    private String currentProviderId = "";
    private String currentModel = "";



    private void initViews(View view) {
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnUpload = view.findViewById(R.id.btnUpload);
        btnNetwork = view.findViewById(R.id.btnNetwork);
        spinnerModel = view.findViewById(R.id.spinnerModel);

        // 初始化模型列表
        modelList = new ArrayList<>();
        modelAdapter = new ArrayAdapter<String>(getActivity(), 
												android.R.layout.simple_spinner_item, modelList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(0xFFFFFFFF);
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setTextColor(0xFFFFFFFF);
                textView.setBackgroundColor(0xFF2D2D2D);
                return textView;
            }
        };
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);
    }

    private void setupClickListeners() {
        // 发送按钮
        btnSend.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					sendMessage();
				}
			});

        // 上传按钮
        btnUpload.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					uploadFile();
				}
			});

        // 联网搜索按钮
        btnNetwork.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleNetworkSearch();
				}
			});

        // 修复：Spinner使用触摸监听而不是点击监听
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

        // 也监听Spinner的点击事件（通过其子视图）
        spinnerModel.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
					// 这里可以处理Spinner正常下拉选择的情况
				}

				@Override
				public void onNothingSelected(android.widget.AdapterView<?> parent) {}
			});
    }

    private void showModelSelector() {
        // 延迟显示避免触摸事件冲突
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showProviderSelector();
					}
				});
        }
    }

    private void showProviderSelector() {
        final List<ApiProvider> providers = loadProviders();
        if (providers.isEmpty()) {
            Toast.makeText(getActivity(), "请先添加API供应商", Toast.LENGTH_SHORT).show();
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

        // 设置对话框暗色主题
        setDialogStyle(dialog);
    }



    private void setDialogStyle(android.app.AlertDialog dialog) {
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
				@Override
				public void onShow(android.content.DialogInterface dialogInterface) {
					android.app.AlertDialog dialog = (android.app.AlertDialog) dialogInterface;

					// 设置背景色
					android.graphics.drawable.ColorDrawable background = new android.graphics.drawable.ColorDrawable(0xFF1E1E1E);
					dialog.getWindow().setBackgroundDrawable(background);

					// 设置按钮颜色
					dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFFFFFFF);
				}
			});
    }


    private void uploadFile() {
        // 实现文件上传逻辑
        Toast.makeText(getActivity(), "上传功能开发中", Toast.LENGTH_SHORT).show();
    }

    private void toggleNetworkSearch() {
        // 切换联网搜索状态
        boolean isEnabled = btnNetwork.getTag() == null || !(boolean) btnNetwork.getTag();
        btnNetwork.setTag(isEnabled);

        if (isEnabled) {
            btnNetwork.setColorFilter(0xFF4CAF50); // 绿色表示启用
            Toast.makeText(getActivity(), "联网搜索已启用", Toast.LENGTH_SHORT).show();
        } else {
            btnNetwork.setColorFilter(0xFF666666); // 灰色表示禁用
            Toast.makeText(getActivity(), "联网搜索已禁用", Toast.LENGTH_SHORT).show();
        }
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







	@Override
	public void onResume() {
		super.onResume();
		// 每次页面显示时刷新模型显示
		refreshModelDisplay();

		// 自动获取焦点
		if (etMessage != null) {
			etMessage.requestFocus();
		}
	}

	public void refreshModelDisplay() {
		if (spinnerModel != null && modelAdapter != null) {
			// 如果当前有选择的模型，确保显示正确
			if (!currentModel.isEmpty()) {
				updateModelSpinner();
			} else {
				// 如果没有选择模型，加载默认模型
				loadAvailableModels();
			}
		}
	}

	// 修改模型更新方法，确保UI线程安全
	private void updateModelSpinner() {
		if (getActivity() == null || spinnerModel == null || modelAdapter == null) return;

		getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						String displayText = currentModel;
						if (displayText.length() > 15) {
							displayText = displayText.substring(0, 15) + "...";
						}

						// 清空并重新添加模型
						modelList.clear();
						modelList.add(displayText);
						modelAdapter.notifyDataSetChanged();

						// 确保Spinner显示正确
						spinnerModel.setSelection(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
	}




	// 保存模型选择到SharedPreferences
	private void saveModelSelection(String providerId, String model) {
		try {
			getActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
				.edit()
				.putString("last_provider_id", providerId)
				.putString("last_model", model)
				.apply();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 加载保存的模型选择
	private void loadSavedModelSelection() {
		try {
			String savedProviderId = getActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
				.getString("last_provider_id", "");
			String savedModel = getActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
				.getString("last_model", "");

			if (!savedProviderId.isEmpty() && !savedModel.isEmpty()) {
				currentProviderId = savedProviderId;
				currentModel = savedModel;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// 加载保存的模型选择
		loadSavedModelSelection();
	}






	// 新增方法：保存供应商到SharedPreferences（用于示例供应商）
	private void saveProviderToPrefs(ApiProvider provider, String providerId) {
		try {
			getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
				.edit()
				.putString(providerId + "_name", provider.getName())
				.putString(providerId + "_url", provider.getApiUrl())
				.putString(providerId + "_key", provider.getApiKey())
				.putInt(providerId + "_model_count", provider.getModels().size())
				.apply();

			// 保存模型列表
			for (int i = 0; i < provider.getModels().size(); i++) {
				getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
					.edit()
					.putString(providerId + "_model_" + i, provider.getModels().get(i))
					.apply();
			}

			// 添加到供应商ID列表
			String existingIds = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
				.getString("provider_ids", "");
			if (!existingIds.isEmpty()) {
				existingIds += "," + providerId;
			} else {
				existingIds = providerId;
			}
			getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
				.edit()
				.putString("provider_ids", existingIds)
				.apply();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}




	private BroadcastReceiver providersUpdateReceiver;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_input, container, false);

		initViews(view);
		setupClickListeners();
		setupBroadcastReceiver(); // 设置广播接收器
		loadAvailableModels();

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		// 注销广播接收器
		if (providersUpdateReceiver != null && getActivity() != null) {
			getActivity().unregisterReceiver(providersUpdateReceiver);
		}
	}

	// 设置广播接收器监听供应商更新
	private void setupBroadcastReceiver() {
		providersUpdateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if ("com.techstar.nexchat.PROVIDERS_UPDATED".equals(intent.getAction())) {
					// 供应商列表已更新，刷新模型选择
					refreshModelSelection();
				}
			}
		};

		// 注册广播接收器
		if (getActivity() != null) {
			IntentFilter filter = new IntentFilter("com.techstar.nexchat.PROVIDERS_UPDATED");
			getActivity().registerReceiver(providersUpdateReceiver, filter);
		}
	}

	// 刷新模型选择
	private void refreshModelSelection() {
		if (getActivity() == null) return;

		getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 检查当前选择的供应商是否还存在
					if (!currentProviderId.isEmpty()) {
						ApiProvider currentProvider = loadProviderById(currentProviderId);
						if (currentProvider == null) {
							// 当前供应商已被删除，重置选择
							currentProviderId = "";
							currentModel = "";
							saveModelSelection("", "");
							updateModelSpinner();
							Toast.makeText(getActivity(), "当前选择的供应商已被删除，请重新选择", Toast.LENGTH_LONG).show();
						} else {
							// 检查当前选择的模型是否还存在
							if (!currentModel.isEmpty() && !currentProvider.getModels().contains(currentModel)) {
								// 当前模型已被删除，重置选择
								currentModel = "";
								saveModelSelection(currentProviderId, "");
								updateModelSpinner();
								Toast.makeText(getActivity(), "当前选择的模型已不存在，请重新选择", Toast.LENGTH_LONG).show();
							}
						}
					}
				}
			});
	}

	// 在InputFragment.java中确保currentProviderId正确设置
	private void showModelSelectorForProvider(final ApiProvider provider) {
		if (provider.getModels().isEmpty()) {
			Toast.makeText(getActivity(), "该供应商没有可用模型", Toast.LENGTH_SHORT).show();
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

					// 保存选择到SharedPreferences
					saveModelSelection(currentProviderId, currentModel);

					updateModelSpinner();
					Toast.makeText(getActivity(), "已选择: " + currentModel, Toast.LENGTH_SHORT).show();

					// 确保ChatFragment也能获取到当前选择
					if (getActivity() instanceof MainActivity) {
						MainActivity mainActivity = (MainActivity) getActivity();
						if (mainActivity.chatFragment != null) {
							mainActivity.chatFragment.setCurrentProviderAndModel(currentProviderId, currentModel);
						}
					}
				}
			})
			.setNegativeButton("取消", null);

		android.app.AlertDialog dialog = builder.create();
		dialog.show();
		setDialogStyle(dialog);
	}



	// 验证供应商数据完整性
	private boolean isProviderValid(ApiProvider provider) {
		return provider != null && 
			provider.getId() != null &&
			!provider.getName().isEmpty() &&
			!provider.getApiUrl().isEmpty() &&
			!provider.getModels().isEmpty();
	}

	// 删除无效的供应商
	private void deleteInvalidProvider(String providerId) {
		try {
			// 从SharedPreferences中删除
			getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
				.edit()
				.remove(providerId + "_name")
				.remove(providerId + "_url")
				.remove(providerId + "_key")
				.remove(providerId + "_model_count")
				.apply();

			// 从ID列表中移除
			String existingIds = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
				.getString("provider_ids", "");
			if (!existingIds.isEmpty()) {
				List<String> idList = new ArrayList<>(Arrays.asList(existingIds.split(",")));
				idList.remove(providerId);
				String newIds = TextUtils.join(",", idList);
				getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
					.edit()
					.putString("provider_ids", newIds)
					.apply();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



    // 修改loadProviders方法，移除示例数据
    private List<ApiProvider> loadProviders() {
        List<ApiProvider> providers = new ArrayList<>();

        try {
            String providerIds = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString("provider_ids", "");

            AppLogger.d("InputFragment", "加载供应商ID列表: " + providerIds);

            if (!providerIds.isEmpty()) {
                String[] ids = providerIds.split(",");
                for (String id : ids) {
                    ApiProvider provider = loadProviderById(id);
                    if (provider != null && !provider.getModels().isEmpty()) {
                        // 确保供应商有ID
                        if (provider.getId() == null) {
                            provider.setId(id);
                        }
                        providers.add(provider);
                        AppLogger.d("InputFragment", "加载供应商: " + provider.getName() + ", ID: " + provider.getId());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 如果没有数据，提示用户添加供应商
        if (providers.isEmpty()) {
            AppLogger.d("InputFragment", "没有找到供应商");
            // 不再添加示例数据，直接返回空列表
            Toast.makeText(getActivity(), "请先添加API供应商", Toast.LENGTH_LONG).show();
        }

        AppLogger.d("InputFragment", "最终供应商数量: " + providers.size());
        return providers;
    }

    // 修改loadAvailableModels方法
    private void loadAvailableModels() {
        // 如果没有选择模型，显示提示
        if (currentModel.isEmpty()) {
            currentModel = "点击选择模型";
            updateModelSpinner();
        }
    }
	

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            // 检查供应商和模型选择
            if (currentProviderId == null || currentProviderId.isEmpty()) {
                AppLogger.e("InputFragment", "供应商ID为空");
                Toast.makeText(getActivity(), "请先选择供应商", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentModel == null || currentModel.isEmpty() || currentModel.equals("点击选择模型")) {
                AppLogger.e("InputFragment", "模型未选择");
                Toast.makeText(getActivity(), "请先选择模型", Toast.LENGTH_SHORT).show();
                return;
            }

            AppLogger.d("InputFragment", "发送消息 - Provider: " + currentProviderId + ", Model: " + currentModel);

            // 发送消息到ChatFragment
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.sendChatMessage(message, currentProviderId, currentModel);

                // 发送后跳转到聊天页面
                mainActivity.switchToChatPage();
            }

            etMessage.setText("");
        }
    }
}
