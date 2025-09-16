package com.techstar.nexchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import com.techstar.nexchat.api.StreamingApiClient;
import com.techstar.nexchat.data.ChatRepo;
import java.io.IOException;
import java.util.Arrays;

public class InputFragment extends Fragment {

    private EditText editText;
    private Button   btnSend;

    public static InputFragment newInstance() {
        return new InputFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.frag_input, container, false);

        editText = (EditText) root.findViewById(R.id.edit);
        btnSend  = (Button)   root.findViewById(R.id.btn_send);

        // 发送按钮
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSend();
            }
        });

        // 工具行壳子事件
        root.findViewById(R.id.toggle_net).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { /* TODO */ }
        });
        root.findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { /* TODO */ }
        });

        // 模型下拉假数据
        Spinner spinner = (Spinner) root.findViewById(R.id.spinner_model);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("moonshot-v1-128k", "kimi-thinking-preview"));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // 弹键盘
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);

        return root;
    }

    private void doSend() {
		final String text = editText.getText().toString().trim();
		if (text.isEmpty()) return;

		long sid = ChatRepo.get(getContext()).currentSession();
		if (sid == -1) {
			String title = text.length() > 20 ? text.substring(0, 20) : text;
			sid = ChatRepo.get(getContext()).createSession(title);
		}
		ChatRepo.get(getContext()).addUser(text);
		LocalBroadcastManager.getInstance(getContext())
            .sendBroadcast(new Intent("chat_changed"));
		editText.setText("");
		((MainActivity) getActivity()).setPage(1);

		/* 流式请求 */
		new android.os.AsyncTask<Void, String, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
				String url = sp.getString("custom_url",  "https://api.openai.com/v1/chat/completions");
				String key = sp.getString("custom_key",  "");
				try {
					new StreamingApiClient(getContext(), url, key).stream(text,
						new StreamingApiClient.DeltaListener() {
							@Override
							public void onDelta(String current) {
								publishProgress(current);
							}
							@Override
							public void onDone(String full) {}
						});
				} catch (IOException e) {
					publishProgress("网络错误: " + e.getMessage());
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(String... values) {
				/* 每次 delta 直接替换最后一条 assistant 消息 */
				Intent i = new Intent("stream_delta");
				i.putExtra("delta", values[0]);
				LocalBroadcastManager.getInstance(getContext()).sendBroadcast(i);
			}
		}.execute();
	}
	
	
}

