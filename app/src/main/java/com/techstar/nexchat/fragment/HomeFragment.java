package com.techstar.nexchat.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.R;
import com.techstar.nexchat.activity.SettingsActivity;
import com.techstar.nexchat.adapter.ChatHistoryAdapter;
import com.techstar.nexchat.adapter.HomeMenuAdapter;
import com.techstar.nexchat.database.ChatHistoryDao;
import com.techstar.nexchat.model.ChatHistory;
import com.techstar.nexchat.model.HomeMenuItem;
import com.techstar.nexchat.util.FileLogger;
import java.util.ArrayList;
import java.util.List;
import com.techstar.nexchat.MainActivity;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    
    private RecyclerView menuRecyclerView;
    private RecyclerView chatHistoryRecyclerView;
    private HomeMenuAdapter menuAdapter;
    private ChatHistoryAdapter chatHistoryAdapter;
    private FileLogger logger;
    private ChatHistoryDao chatHistoryDao;
    
    private List<HomeMenuItem> menuItems;
    private List<ChatHistory> chatHistories;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        logger = FileLogger.getInstance(getContext());
        chatHistoryDao = new ChatHistoryDao(getContext());
        
        logger.i(TAG, "HomeFragment created");
        
        initViews(view);
        initMenuItems();
        loadChatHistory();
        
        return view;
    }
    
    private void initViews(View view) {
        menuRecyclerView = view.findViewById(R.id.menuRecyclerView);
        chatHistoryRecyclerView = view.findViewById(R.id.chatHistoryList);
        
        // 初始化菜单列表
        menuItems = new ArrayList<>();
        menuAdapter = new HomeMenuAdapter(getContext(), menuItems);
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuRecyclerView.setAdapter(menuAdapter);
        
        // 初始化聊天历史列表
        chatHistories = new ArrayList<>();
        chatHistoryAdapter = new ChatHistoryAdapter(getContext(), chatHistories);
        chatHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatHistoryRecyclerView.setAdapter(chatHistoryAdapter);
        
        // 设置聊天历史点击监听
        chatHistoryAdapter.setOnItemClickListener(new ChatHistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ChatHistory chatHistory) {
                logger.i(TAG, "Chat history clicked: " + chatHistory.getTitle());
                // 切换到聊天页面并加载该对话
                switchToChat(chatHistory.getId());
            }
            
            @Override
            public void onItemLongClick(ChatHistory chatHistory) {
                logger.i(TAG, "Chat history long clicked: " + chatHistory.getTitle());
                // 显示删除对话框
                showDeleteChatDialog(chatHistory);
            }
        });
    }
    
    private void initMenuItems() {
        menuItems.clear();
        
        // 添加菜单项
        menuItems.add(new HomeMenuItem("新建对话", R.drawable.ic_add_white_24dp, new HomeMenuItem.MenuItemClickListener() {
            @Override
            public void onClick() {
                createNewChat();
            }
        }));
        
        menuItems.add(new HomeMenuItem("设置", R.drawable.ic_settings_white_24dp, new HomeMenuItem.MenuItemClickListener() {
            @Override
            public void onClick() {
                openSettings();
            }
        }));
        
        menuAdapter.notifyDataSetChanged();
    }
    
    private void loadChatHistory() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<ChatHistory> histories = chatHistoryDao.getAllChats();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chatHistories.clear();
                            chatHistories.addAll(histories);
                            chatHistoryAdapter.notifyDataSetChanged();
                            logger.i(TAG, "Loaded " + histories.size() + " chat histories");
                        }
                    });
                }
            }
        }).start();
    }
    
    private void createNewChat() {
        logger.i(TAG, "Creating new chat");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChatHistory newChat = new ChatHistory("新对话");
                long chatId = chatHistoryDao.insertChat(newChat);
                
                if (chatId != -1) {
                    newChat.setId((int) chatId);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 添加到列表顶部
                                chatHistories.add(0, newChat);
                                chatHistoryAdapter.notifyItemInserted(0);
                                chatHistoryRecyclerView.scrollToPosition(0);
                                
                                // 切换到该对话
                                switchToChat((int) chatId);
                            }
                        });
                    }
                }
            }
        }).start();
    }
    
    private void openSettings() {
        logger.i(TAG, "Opening settings");
        Intent intent = new Intent(getContext(), SettingsActivity.class);
        startActivity(intent);
    }
    
    private void switchToChat(int chatId) {
        logger.i(TAG, "Switching to chat: " + chatId);
        // 通知 ChatFragment 切换对话
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.switchToChatPage();
            
            // 这里需要通过某种方式通知 ChatFragment 加载指定对话
            // 可以使用 EventBus 或者接口回调，这里先用简单方式
        }
    }
    
    private void showDeleteChatDialog(final ChatHistory chatHistory) {
        // 简单的确认对话框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("删除对话")
                .setMessage("确定要删除对话 \"" + chatHistory.getTitle() + "\" 吗？")
                .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        deleteChat(chatHistory);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void deleteChat(final ChatHistory chatHistory) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = chatHistoryDao.deleteChat(chatHistory.getId());
                if (success) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int position = chatHistories.indexOf(chatHistory);
                                if (position != -1) {
                                    chatHistories.remove(position);
                                    chatHistoryAdapter.notifyItemRemoved(position);
                                    logger.i(TAG, "Deleted chat: " + chatHistory.getTitle());
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 重新加载数据，确保显示最新状态
        loadChatHistory();
    }
}
