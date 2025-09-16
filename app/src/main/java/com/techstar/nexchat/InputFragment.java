package com.techstar.nexchat;

public class InputFragment extends Fragment {
    public static InputFragment newInstance() { return new InputFragment(); }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frag_input, container, false);
        EditText edit = root.findViewById(R.id.edit);
        edit.requestFocus();
        // 监听返回键
        root.setFocusableInTouchMode(true);
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                String text = edit.getText().toString().trim();
                if (!text.isEmpty()) {
                    // 插入数据库 & 调用 AI
                    ChatRepo.get(getContext()).addUser(text);
                    ApiClient.get().send(text, null);
                }
                // 回到聊天页
                ((MainActivity) getActivity()).setPage(1);
                return true;
            }
            return false;
        });
        return root;
    }
}
