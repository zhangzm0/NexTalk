package com.techstar.nexchat.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.techstar.nexchat.R;
import com.techstar.nexchat.database.ApiProviderDao;
import com.techstar.nexchat.database.MessageDao;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.model.StreamingMessage;
import com.techstar.nexchat.service.ApiClient;
import com.techstar.nexchat.service.UIUpdateCoordinator;
import com.techstar.nexchat.util.FileLogger;
import java.util.ArrayList;
import java.util.List;

public class InputFragment extends Fragment {
    private static final String TAG = "InputFragment";
    
    private EditText etMessage;
    private ImageButton btnSend;
    private Spinner spinnerModel;
    private ApiProviderDao apiProviderDao;
    private FileLogger logger;
    
    private List<ApiProvider> apiProviders;
    private List<String> availableModels;
    private ArrayAdapter<String> modelAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container, false);
        logger = FileLogger.getInstance(getContext());
        apiProviderDao = new ApiProviderDao(getContext());
        
        logger.i(TAG, "InputFragment created");
        
        initViews(view);
        loadApiProviders();
        
        return view;
    }
    
    private void initViews(View view) {
		etMessage = view.findViewById(R.id.etMessage);
		btnSend = (android.widget.ImageButton) view.findViewById(R.id.btnSend);
		spinnerModel = view.findViewById(R.id.spinnerModel);

		// 初始化模型下拉列表
		availableModels = new ArrayList<>();
		modelAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, availableModels);
		modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerModel.setAdapter(modelAdapter);

		// 加载保存的模型选择
		loadSelectedModel();

		// 发送按钮点击事件
		btnSend.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					sendMessage();
				}
			});

		// 监听模型选择变化，保存选择
		spinnerModel.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
					saveSelectedModel(position);
				}

				@Override
				public void onNothingSelected(android.widget.AdapterView<?> parent) {
				}
			});

		// 监听输入框回车键
		etMessage.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(android.widget.TextView v, int actionId, android.view.KeyEvent event) {
					if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
						sendMessage();
						return true;
					}
					return false;
				}
			});
	}
	
	private void saveSelectedModel(int position) {
		if (position >= 0 && position < availableModels.size()) {
			String selectedModel = availableModels.get(position);
			android.content.SharedPreferences prefs = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
			prefs.edit().putString("selected_model", selectedModel).apply();
			logger.d(TAG, "Saved selected model: " + selectedModel);
		}
	}

	private void loadSelectedModel() {
		android.content.SharedPreferences prefs = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
		String savedModel = prefs.getString("selected_model", "");

		if (!savedModel.isEmpty() && availableModels.contains(savedModel)) {
			int position = availableModels.indexOf(savedModel);
			spinnerModel.setSelection(position);
			logger.d(TAG, "Loaded saved model: " + savedModel);
		}
	}
    
    private void loadApiProviders() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                apiProviders = apiProviderDao.getAllProviders();
                updateModelsList();
            }
        }).start();
    }
    
    private void updateModelsList() {
		availableModels.clear();

		for (ApiProvider provider : apiProviders) {
			if (provider.getModels() != null) {
				for (String model : provider.getModels()) {
					// 格式：供应商名 - 模型名
					availableModels.add(provider.getName() + " - " + model);
				}
			}
		}

		// 如果没有模型，添加一个默认提示
		if (availableModels.isEmpty()) {
			availableModels.add("请先添加API供应商");
		}

		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						modelAdapter.notifyDataSetChanged();
						// 重新加载保存的模型选择
						loadSelectedModel();
						logger.i(TAG, "Updated models list, count: " + availableModels.size());
					}
				});
		}
	}
    
    // 删除整个 simulateAIResponse 方法
	

// 替换当前的 sendMessage 方法
	// 替换 InputFragment.java 中的 sendMessage 方法：
	// 完全替换 InputFragment.java 中的 sendMessage 方法：
	private void sendMessage() {
		String message = etMessage.getText().toString().trim();
		if (TextUtils.isEmpty(message)) {
			android.widget.Toast.makeText(getContext(), "请输入消息", android.widget.Toast.LENGTH_SHORT).show();
			return;
		}

		// 获取选中的模型
		String selectedModelWithProvider = (String) spinnerModel.getSelectedItem();
		if (selectedModelWithProvider == null || selectedModelWithProvider.equals("请先添加API供应商")) {
			android.widget.Toast.makeText(getContext(), "请先添加API供应商并获取模型", android.widget.Toast.LENGTH_SHORT).show();
			return;
		}

		// 解析供应商和模型名称
		String[] parts = selectedModelWithProvider.split(" - ");
		if (parts.length != 2) {
			android.widget.Toast.makeText(getContext(), "模型格式错误", android.widget.Toast.LENGTH_SHORT).show();
			return;
		}

		final String providerName = parts[0];
		final String modelName = parts[1];

		// 清空输入框
		etMessage.setText("");

		// 获取 ChatFragment
		final ChatFragment chatFragment = getChatFragment();
		if (chatFragment != null) {
			// 立即添加用户消息
			chatFragment.addUserMessage(message);

			// 直接启动API调用线程（不等待用户消息完成）
			new Thread(new Runnable() {
					@Override
					public void run() {
						callRealAPI(chatFragment, providerName, modelName);
					}
				}).start();

		} else {
			logger.w(TAG, "ChatFragment is null, cannot send message");
			android.widget.Toast.makeText(getContext(), "无法发送消息，请重试", android.widget.Toast.LENGTH_SHORT).show();
		}

		logger.i(TAG, "Sent message: " + message + ", model: " + selectedModelWithProvider);
	}

	private void callRealAPI(final ChatFragment chatFragment, final String providerName, final String modelName) {
		try {
			// 获取API供应商信息
			ApiProvider provider = apiProviderDao.getProviderByName(providerName);

			if (provider == null) {
				showToast("API供应商未找到: " + providerName);
				return;
			}

			logger.i(TAG, "Found provider: " + provider.getName() + ", URL: " + provider.getApiUrl());

			// 获取当前聊天的历史消息
			int chatId = chatFragment.getCurrentChatId();
			com.techstar.nexchat.database.MessageDao messageDao = 
				new com.techstar.nexchat.database.MessageDao(getContext());
			List<com.techstar.nexchat.model.Message> historyMessages = 
				messageDao.getMessagesByChatId(chatId);

			logger.i(TAG, "Starting real API call with " + historyMessages.size() + " history messages, chatId: " + chatId);

			// 创建AI客户端并调用流式API
			ApiClient apiClient = new ApiClient(getContext());

			// 添加空的助理消息并获取其ID
			final int pendingMessageId = chatFragment.addAssistantMessage("", modelName, true);

			logger.i(TAG, "Created pending assistant message with ID: " + pendingMessageId);

			if (pendingMessageId != -1) {
				// 执行流式聊天请求
				apiClient.streamChat(provider, modelName, historyMessages, 
					chatId, pendingMessageId, 
					new ApiClient.ChatCallback() {
						@Override
						public void onResponse(String contentChunk) {
							logger.d(TAG, "Received stream chunk: " + contentChunk);
							// 实时更新消息内容
							if (getActivity() != null) {
								getActivity().runOnUiThread(new Runnable() {
										@Override
										public void run() {
											UIUpdateCoordinator uiCoordinator = 
												UIUpdateCoordinator.getInstance(logger);
											uiCoordinator.updateMessageContent(chatId, pendingMessageId, contentChunk);
										}
									});
							}
						}

						@Override
						public void onError(String error) {
							logger.e(TAG, "API call error: " + error);
							showToast("请求失败: " + error);

							// 更新消息状态为错误
							if (getActivity() != null) {
								getActivity().runOnUiThread(new Runnable() {
										@Override
										public void run() {
											chatFragment.completeAssistantMessage(pendingMessageId, "错误: " + error);
										}
									});
							}
						}

						@Override
						public void onComplete() {
							logger.i(TAG, "API call completed successfully for message: " + pendingMessageId);

							// 获取完整内容并完成消息
							UIUpdateCoordinator uiCoordinator = UIUpdateCoordinator.getInstance(logger);
							StreamingMessage streamingMessage = uiCoordinator.getStreamingMessage(pendingMessageId);
							if (streamingMessage != null) {
								final String fullContent = streamingMessage.getFullContent();
								if (getActivity() != null) {
									getActivity().runOnUiThread(new Runnable() {
											@Override
											public void run() {
												chatFragment.completeAssistantMessage(pendingMessageId, fullContent);
											}
										});
								}
							}
						}

						@Override
						public void onStreamStart() {
							logger.d(TAG, "Stream started for message: " + pendingMessageId);
						}

						@Override
						public void onStreamEnd() {
							logger.d(TAG, "Stream ended for message: " + pendingMessageId);
						}
					});
			} else {
				logger.e(TAG, "Failed to create pending assistant message");
			}

		} catch (Exception e) {
			logger.e(TAG, "Failed to call API", e);
			showToast("API调用失败: " + e.getMessage());
		}
	}

// 添加工具方法
	private void showToast(final String message) {
		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
					}
				});
		}
	}


    
    
    
    private ChatFragment getChatFragment() {
		if (getActivity() != null) {
			// 通过 FragmentManager 获取 ChatFragment
			androidx.fragment.app.FragmentManager fragmentManager = getParentFragmentManager();

			// 尝试通过 tag 查找
			Fragment fragment = fragmentManager.findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + 1);
			if (fragment instanceof ChatFragment) {
				return (ChatFragment) fragment;
			}

			// 如果上面方法不行，尝试遍历所有 fragment
			List<Fragment> fragments = fragmentManager.getFragments();
			for (Fragment frag : fragments) {
				if (frag instanceof ChatFragment) {
					return (ChatFragment) frag;
				}
			}
		}
		return null;
	}
    
    public void refreshModels() {
        loadApiProviders();
    }
}
