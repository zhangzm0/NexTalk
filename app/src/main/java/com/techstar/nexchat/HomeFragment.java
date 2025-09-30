package com.techstar.nexchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.model.ChatConversation;
import com.techstar.nexchat.model.HomeMenuItem;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private HomeAdapter adapter;
    private List<Object> items;
    private ChatManager chatManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        chatManager = ChatManager.getInstance(getActivity());
        initRecyclerView(view);
        loadData();

        return view;
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.chatHistoryList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        items = new ArrayList<>();
        adapter = new HomeAdapter(items);
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        items.clear();

        // 只保留必要的菜单项，并设置正确的图标
        items.add(new HomeMenuItem(R.drawable.ic_add_white_24dp, "新建对话", new View.OnClickListener() {
						  @Override
						  public void onClick(View v) {
							  createNewChat();
						  }
					  }));

        items.add(new HomeMenuItem(R.drawable.ic_delete_white_24dp, "清空所有对话", new View.OnClickListener() {
						  @Override
						  public void onClick(View v) {
							  clearAllChats();
						  }
					  }));

        // 设置图标
        items.add(new HomeMenuItem(R.drawable.ic_settings_white_24dp, "设置", new View.OnClickListener() {
						  @Override
						  public void onClick(View v) {
							  openSettings();
						  }
					  }));

        // 添加分隔符
        items.add("分隔符");

        // 添加聊天历史
        loadChatHistory();

        adapter.notifyDataSetChanged();
    }

    private void createNewChat() {
        ChatConversation newConversation = chatManager.createNewConversation();
        switchToChatAndLoad(newConversation.getId());
    }

    private void clearAllChats() {
        new android.app.AlertDialog.Builder(getActivity())
            .setTitle("确认清空")
            .setMessage("确定要清空所有对话记录吗？此操作不可恢复！")
            .setPositiveButton("清空", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    chatManager.clearAllConversations();
                    loadData(); // 重新加载数据
                    Toast.makeText(getActivity(), "已清空所有对话", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "打开设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadChatHistory() {
        List<ChatConversation> conversations = chatManager.loadAllConversations();
        for (ChatConversation conversation : conversations) {
            items.add(conversation);
        }
    }

    private void switchToChatAndLoad(String conversationId) {
        chatManager.setCurrentConversation(conversationId);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToChatPage();
        }
    }

    // 显示对话操作菜单
    private void showConversationMenu(final ChatConversation conversation) {
        final String[] items = conversation.isPinned() ? 
            new String[]{"取消置顶", "删除对话", "重命名"} : 
            new String[]{"置顶", "删除对话", "重命名"};

        new android.app.AlertDialog.Builder(getActivity())
            .setTitle(conversation.getTitle())
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: // 置顶/取消置顶
                            chatManager.togglePinConversation(conversation.getId());
                            loadData(); // 重新加载
                            break;
                        case 1: // 删除
                            deleteConversation(conversation);
                            break;
                        case 2: // 重命名
                            renameConversation(conversation);
                            break;
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void deleteConversation(final ChatConversation conversation) {
        new android.app.AlertDialog.Builder(getActivity())
            .setTitle("确认删除")
            .setMessage("确定要删除对话 \"" + conversation.getTitle() + "\" 吗？")
            .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    chatManager.deleteConversation(conversation.getId());
                    loadData(); // 重新加载
                    Toast.makeText(getActivity(), "对话已删除", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void renameConversation(final ChatConversation conversation) {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setTitle("重命名对话");

        final EditText input = new EditText(getActivity());
        input.setText(conversation.getTitle());
        input.setTextColor(0xFFFFFFFF);
        input.setBackgroundColor(0xFF1E1E1E);
        input.setPadding(20, 20, 20, 20);
        builder.setView(input);

        builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(android.content.DialogInterface dialog, int which) {
					String newTitle = input.getText().toString().trim();
					if (!newTitle.isEmpty() && !newTitle.equals(conversation.getTitle())) {
						conversation.setTitle(newTitle);
						chatManager.saveConversation(conversation);
						loadData(); // 重新加载
					}
				}
			});
        builder.setNegativeButton("取消", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 修改HomeAdapter支持长按操作
    private class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Object> items;

        public HomeAdapter(List<Object> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 这里需要根据viewType创建不同的ViewHolder
            // 实现细节根据你的实际布局文件来定
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = items.get(position);
            if (holder instanceof ChatHistoryViewHolder && item instanceof ChatConversation) {
                ((ChatHistoryViewHolder) holder).bind((ChatConversation) item);
            }
            // 其他类型的ViewHolder绑定逻辑
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            // 根据item类型返回不同的viewType
            Object item = items.get(position);
            if (item instanceof HomeMenuItem) return 0;
            if (item instanceof String && "分隔符".equals(item)) return 1;
            if (item instanceof ChatConversation) return 2;
            return -1;
        }

        private class ChatHistoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvChatTitle, tvPreview, tvTime;
            ImageView ivPin;

            public ChatHistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvChatTitle = itemView.findViewById(R.id.tvChatTitle);
                tvPreview = itemView.findViewById(R.id.tvPreview);
                tvTime = itemView.findViewById(R.id.tvTime);
                ivPin = itemView.findViewById(R.id.ivPin);
            }

            public void bind(final ChatConversation conversation) {
                tvChatTitle.setText(conversation.getTitle());
                tvPreview.setText(conversation.getPreview());
                tvTime.setText(conversation.getFormattedTime());

                // 显示置顶图标
                if (conversation.isPinned()) {
                    ivPin.setVisibility(View.VISIBLE);
                } else {
                    ivPin.setVisibility(View.GONE);
                }

                // 点击进入对话
                itemView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							switchToChatAndLoad(conversation.getId());
						}
					});

                // 长按显示操作菜单
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							showConversationMenu(conversation);
							return true;
						}
					});
            }
        }
    }
}

