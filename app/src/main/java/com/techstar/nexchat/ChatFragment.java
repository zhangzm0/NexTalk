package com.techstar.nexchat;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.TextView;

public class ChatFragment extends Fragment {
    public static ChatFragment newInstance(long sessionId) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putLong("sid", sessionId);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        // 空布局，后面再填
        return i.inflate(R.layout.frag_chat, c, false);
    }
}

