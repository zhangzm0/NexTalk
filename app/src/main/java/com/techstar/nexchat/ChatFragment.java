package com.techstar.nexchat;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ChatFragment extends Fragment {

    private TextView tvChatTitle, tvChatContent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        tvChatTitle = (TextView) view.findViewById(R.id.tvChatTitle);
        tvChatContent = (TextView) view.findViewById(R.id.tvChatContent);

        return view;
    }

    public void updateChatContent(String content) {
        if (tvChatContent != null) {
            tvChatContent.setText(content);
        }
    }

    public void setChatTitle(String title) {
        if (tvChatTitle != null) {
            tvChatTitle.setText(title);
        }
    }
}
