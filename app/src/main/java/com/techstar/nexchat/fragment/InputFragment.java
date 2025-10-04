package com.techstar.nexchat.fragment;

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
import com.techstar.nexchat.model.ApiProvider;
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

		// 清空输入框
		etMessage.setText("");

		// 直接获取 ChatFragment 并发送消息
		ChatFragment chatFragment = getChatFragment();
		if (chatFragment != null) {
			chatFragment.addUserMessage(message);

			// 模拟AI回复
			simulateAIResponse(chatFragment, message, selectedModelWithProvider);
		} else {
			logger.w(TAG, "ChatFragment is null, cannot send message");
			android.widget.Toast.makeText(getContext(), "无法发送消息，请重试", android.widget.Toast.LENGTH_SHORT).show();
		}

		logger.i(TAG, "Sent message: " + message + ", model: " + selectedModelWithProvider);
	}
    
    private void simulateAIResponse(final ChatFragment chatFragment, final String userMessage, final String model) {
        // 模拟网络延迟
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String response = "这是AI的模拟回复。你说了: \"" + userMessage + "\"\n\n" +
                        "这是一个**加粗**的文本，*斜体*文本，还有`行内代码`。\n\n" +
                        "支持基本的 Markdown 格式渲染。";
                
                chatFragment.addAssistantMessage(response, model);
                logger.i(TAG, "Simulated AI response for: " + userMessage);
            }
        }, 1000);
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
