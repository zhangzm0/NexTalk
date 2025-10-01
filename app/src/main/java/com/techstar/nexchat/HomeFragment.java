package com.techstar.nexchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        // 添加菜单项
        items.add(new HomeMenuItem(android.R.drawable.ic_menu_add, "新建对话", new View.OnClickListener() {
						  @Override
						  public void onClick(View v) {
							  createNewChat();
						  }
					  }));

        items.add(new HomeMenuItem(android.R.drawable.ic_menu_delete, "清空所有对话", new View.OnClickListener() {
						  @Override
						  public void onClick(View v) {
							  clearAllChats();
						  }
					  }));

        items.add(new HomeMenuItem(android.R.drawable.ic_menu_preferences, "设置", new View.OnClickListener() {
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

    private void loadChatHistory() {
        List<ChatConversation> conversations = chatManager.loadAllConversations();
        for (ChatConversation conversation : conversations) {
            items.add(conversation);
        }
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
                    loadData();
                    Toast.makeText(getActivity(), "已清空所有对话", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void openSettings() {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
    }

    private void switchToChatAndLoad(String conversationId) {
        chatManager.setCurrentConversation(conversationId);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToChatPage();
        }
    }

    // HomeAdapter内部类
    private class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_MENU_ITEM = 0;
        private static final int TYPE_CHAT_HISTORY = 1;
        private static final int TYPE_SEPARATOR = 2;

        private List<Object> items;

        public HomeAdapter(List<Object> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            Object item = items.get(position);
            if (item instanceof HomeMenuItem) {
                return TYPE_MENU_ITEM;
            } else if (item instanceof ChatConversation) {
                return TYPE_CHAT_HISTORY;
            } else if (item instanceof String && ((String) item).equals("分隔符")) {
                return TYPE_SEPARATOR;
            }
            return TYPE_MENU_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            switch (viewType) {
                case TYPE_MENU_ITEM:
                    View menuView = inflater.inflate(R.layout.item_home_menu, parent, false);
                    return new MenuItemViewHolder(menuView);

                case TYPE_CHAT_HISTORY:
                    View historyView = inflater.inflate(R.layout.item_chat_history, parent, false);
                    return new ChatHistoryViewHolder(historyView);

                case TYPE_SEPARATOR:
                    View separatorView = inflater.inflate(R.layout.item_separator, parent, false);
                    return new SeparatorViewHolder(separatorView);

                default:
                    // 备用方案
                    TextView textView = new TextView(parent.getContext());
                    return new SimpleViewHolder(textView);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position < 0 || position >= items.size()) {
                return;
            }

            Object item = items.get(position);

            switch (holder.getItemViewType()) {
                case TYPE_MENU_ITEM:
                    ((MenuItemViewHolder) holder).bind((HomeMenuItem) item);
                    break;

                case TYPE_CHAT_HISTORY:
                    ((ChatHistoryViewHolder) holder).bind((ChatConversation) item);
                    break;

                case TYPE_SEPARATOR:
                    ((SeparatorViewHolder) holder).bind();
                    break;

                default:
                    // 什么都不做
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        // 菜单项ViewHolder
        private class MenuItemViewHolder extends RecyclerView.ViewHolder {
            private android.widget.ImageView icon;
            private TextView title;

            public MenuItemViewHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.icon);
                title = itemView.findViewById(R.id.title);
            }

            public void bind(final HomeMenuItem menuItem) {
                if (icon != null) {
                    icon.setImageResource(menuItem.getIconRes());
                }
                if (title != null) {
                    title.setText(menuItem.getTitle());
                }

                itemView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (menuItem.getOnClickListener() != null) {
								menuItem.getOnClickListener().onClick(v);
							}
						}
					});
            }
        }

        // 聊天历史ViewHolder
        private class ChatHistoryViewHolder extends RecyclerView.ViewHolder {
            private TextView tvChatTitle, tvPreview, tvTime, tvMessageCount;
            private android.widget.ImageView ivPin;

            public ChatHistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvChatTitle = itemView.findViewById(R.id.tvChatTitle);
                tvPreview = itemView.findViewById(R.id.tvPreview);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvMessageCount = itemView.findViewById(R.id.tvMessageCount);
                ivPin = itemView.findViewById(R.id.ivPin);
            }

            public void bind(final ChatConversation conversation) {
                if (tvChatTitle != null) {
                    tvChatTitle.setText(conversation.getTitle());
                }
                if (tvPreview != null) {
                    tvPreview.setText(conversation.getPreview());
                }
                if (tvTime != null) {
                    tvTime.setText(conversation.getFormattedTime());
                }
                if (tvMessageCount != null) {
                    tvMessageCount.setText(conversation.getMessageCount() + "条");
                }
                if (ivPin != null) {
                    if (conversation.isPinned()) {
                        ivPin.setVisibility(View.VISIBLE);
                    } else {
                        ivPin.setVisibility(View.GONE);
                    }
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

        // 分隔符ViewHolder
        private class SeparatorViewHolder extends RecyclerView.ViewHolder {
            private View separator;

            public SeparatorViewHolder(@NonNull View itemView) {
                super(itemView);
                separator = itemView.findViewById(R.id.separator);
            }

            public void bind() {
                // 分隔符不需要特殊处理
            }
        }

        // 简单的ViewHolder作为备用
        private class SimpleViewHolder extends RecyclerView.ViewHolder {
            private TextView textView;

            public SimpleViewHolder(@NonNull View itemView) {
                super(itemView);
                if (itemView instanceof TextView) {
                    textView = (TextView) itemView;
                } else {
                    textView = new TextView(itemView.getContext());
                }
            }
        }
    }

    // 显示对话操作菜单
    private void showConversationMenu(final ChatConversation conversation) {
        new android.app.AlertDialog.Builder(getActivity())
            .setTitle(conversation.getTitle())
            .setItems(new String[]{"置顶", "删除对话", "重命名"}, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: // 置顶/取消置顶
                            chatManager.togglePinConversation(conversation.getId());
                            loadData();
                            String action = conversation.isPinned() ? "取消置顶" : "置顶";
                            Toast.makeText(getActivity(), "已" + action, Toast.LENGTH_SHORT).show();
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
                    loadData();
                    Toast.makeText(getActivity(), "对话已删除", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void renameConversation(final ChatConversation conversation) {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setTitle("重命名对话");

        final android.widget.EditText input = new android.widget.EditText(getActivity());
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
						loadData();
					}
				}
			});
        builder.setNegativeButton("取消", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
}
