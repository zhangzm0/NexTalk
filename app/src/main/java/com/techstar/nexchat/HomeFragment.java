package com.techstar.nexchat;

public class HomeFragment extends Fragment {
    public static HomeFragment newInstance() { return new HomeFragment(); }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frag_home, container, false);
        // 新建对话
        root.findViewById(R.id.btn_new).setOnClickListener(v -> {
            // 清数据库、跳聊天页
            ChatRepo.get(getContext()).clear();
            ((MainActivity) getActivity()).setPage(1);
        });
        // 历史列表（ListView）
        ListView history = root.findViewById(R.id.list_history);
        history.setAdapter(new SimpleAdapter(...));
        history.setOnItemClickListener((p, v, pos, id) -> {
            // 恢复会话
            ((MainActivity) getActivity()).setPage(1);
        });
        // 设置
        root.findViewById(R.id.btn_settings).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SettingsActivity.class)));
        return root;
    }
}