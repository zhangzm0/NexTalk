package com.techstar.nexchat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import com.techstar.nexchat.data.ChatRepo;

public class InputFragment extends Fragment {

    private EditText editText;

    public static InputFragment newInstance() {
        return new InputFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.frag_input, container, false);
        editText = (EditText) root.findViewById(R.id.edit);

        // 弹出软键盘
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);

        // 按“完成”键也触发发送
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendAndBack();
                    return true;
                }
                return false;
            }
        });

        // 拦截物理返回键
        root.setFocusableInTouchMode(true);
        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    sendAndBack();
                    return true;
                }
                return false;
            }
        });

        return root;
    }

    private void sendAndBack() {
        String text = editText.getText().toString().trim();
        if (!text.isEmpty()) {
            // 1. 入库
            ChatRepo.get(getContext()).addUser(text);
            // 2. 通知聊天页刷新
            LocalBroadcastManager.getInstance(getContext())
                    .sendBroadcast(new Intent("chat_changed"));
            // 3. 清输入框
            editText.setText("");
        }
        // 4. 回到聊天页
        ((MainActivity) getActivity()).setPage(1);
    }
}

