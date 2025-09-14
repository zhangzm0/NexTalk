package com.techstar.nexchat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.*;

public class InputFragment extends Fragment {
    private EditText inputEdit;
    private ImageButton btnSend, btnModel, btnAttach, btnNet;
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frag_input, container, false);

        inputEdit = (EditText) v.findViewById(R.id.input_edit);
        btnSend   = (ImageButton) v.findViewById(R.id.btn_send);
        btnModel  = (ImageButton) v.findViewById(R.id.btn_model);
        btnAttach = (ImageButton) v.findViewById(R.id.btn_attach);
        btnNet    = (ImageButton) v.findViewById(R.id.btn_net);

        btnSend.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) { sendText(); }
			});
        btnModel.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) { chooseModel(); }
			});
        btnAttach.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) { pickFile(); }
			});
        btnNet.setOnClickListener(new View.OnClickListener() {
				boolean isOn = false;
				public void onClick(View v) {
					isOn = !isOn;
					btnNet.setColorFilter(isOn ? 0xFF00F8EA : 0xFFE0E0E0); // 开=accent 关=fg
					setNetEnabled(isOn);
				}
			});
        return v;
    }

    /* 简单回调：把事件抛给宿主 Activity -------------------- */
    private void sendText() {
        String text = inputEdit.getText().toString().trim();
        if (text.isEmpty()) return;
        inputEdit.setText("");
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onUserInput(text);
        }
    }
    private void chooseModel() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onChooseModel();
        }
    }
    private void setNetEnabled(boolean on) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onNetToggle(on);
        }
    }
    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == getActivity().RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onFilePicked(uri);
            }
        }
    }
}

