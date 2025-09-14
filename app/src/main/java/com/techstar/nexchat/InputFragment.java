package com.techstar.nexchat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;

public class InputFragment extends Fragment {
    private EditText inputEdit;
    private ImageButton btnSend, btnModel, btnAttach;
    private ToggleButton toggleNet;

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        View v = i.inflate(R.layout.frag_input, c, false);
        inputEdit = v.findViewById(R.id.input_edit);
        btnSend   = v.findViewById(R.id.btn_send);
        btnModel  = v.findViewById(R.id.btn_model);
        btnAttach = v.findViewById(R.id.btn_attach);
        toggleNet = v.findViewById(R.id.toggle_net);

        btnSend.setOnClickListener(   v1 -> sendText() );
        btnModel.setOnClickListener(  v1 -> chooseModel() );
        btnAttach.setOnClickListener( v1 -> pickFile() );
        toggleNet.setOnCheckedChangeListener((btn, isOn) -> setNetEnabled(isOn));
        return v;
    }

    /* 回调给 Activity 统一处理 */
    private void sendText() {
        String text = inputEdit.getText().toString().trim();
        if (text.isEmpty()) return;
        inputEdit.setText("");
        ((MainActivity) getActivity()).onUserInput(text);   // 回调
    }
    private void chooseModel() {
        ((MainActivity) getActivity()).onChooseModel();
    }
    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, 1);
    }
    private void setNetEnabled(boolean on) {
        ((MainActivity) getActivity()).onNetToggle(on);
    }

    @Override
    public void onActivityResult(int req, int res, Intent data) {
        if (req == 1 && res == RESULT_OK && data != null)
            ((MainActivity) getActivity()).onFilePicked(data.getData());
    }
}

