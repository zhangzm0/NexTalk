
package com.techstar.nexchat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HomeFragment extends Fragment {
    private SessionAdapter adapter;
    private RecyclerView list;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frag_home, container, false);
        list = (RecyclerView) v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SessionAdapter(getContext());
        list.setAdapter(adapter);

        reload(); // 首次加载

        adapter.setOnItemClickListener(new SessionAdapter.OnItemClickListener() {
				public void onItemClick(Session s) {
					((MainActivity) getActivity()).openChat(s.id);
				}
			});
        adapter.setOnItemLongListener(new SessionAdapter.OnItemLongListener() {
				public boolean onItemLongClick(Session s) {
					SessionDAO.delete(getContext(), s.id);
					reload(); // 刷新
					return true;
				}
			});
        return v;
    }

    private void reload() {
        android.database.Cursor c = SessionDAO.queryAll(getContext());
        adapter.reload(c);
        c.close();
    }
}


