package com.techstar.nexchat;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.TextView;

public class ChatFragment extends Fragment {
    private View appBar;
    private boolean isBarShow = true;

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        View root = i.inflate(R.layout.frag_chat, c, false);
        appBar = root.findViewById(R.id.appbar);
        RecyclerView list = root.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(new EmptyAdapter()); // 占位
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    if (dy > 5 && isBarShow) hideBar();
                    if (dy < -5 && !isBarShow) showBar();
                }
            });
        return root;
    }

    private void hideBar() {
        isBarShow = false;
        appBar.animate().translationY(-appBar.getHeight()).alpha(0f).setDuration(100).start();
    }

    private void showBar() {
        isBarShow = true;
        appBar.animate().translationY(0).alpha(1f).setDuration(100).start();
    }

    /* 占位 Adapter，后续替换为真实消息 Adapter */
    private static class EmptyAdapter extends RecyclerView.Adapter<EmptyAdapter.H> {
        static class H extends RecyclerView.ViewHolder {
            H(View v) { super(v); }
        }
        @Override public int getItemCount() { return 0; }
        @Override public H onCreateViewHolder(ViewGroup p, int t) {
            return new H(new TextView(p.getContext()));
        }
        @Override public void onBindViewHolder(H h, int p) {}
    }
}

