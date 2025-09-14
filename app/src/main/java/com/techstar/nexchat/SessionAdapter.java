package com.techstar.nexchat;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.Holder> {
    private final LayoutInflater inflater;
    private final List<Session> data = new ArrayList<>();
    private OnItemClickListener clickListener;
    private OnItemLongListener longListener;

    public interface OnItemClickListener { void onItemClick(Session s); }
    public interface OnItemLongListener { boolean onItemLongClick(Session s); }

    public SessionAdapter(Context c) { inflater = LayoutInflater.from(c); }

    public void reload(Cursor c) {
        data.clear();
        while (c.moveToNext()) {
            Session s = new Session();
            s.id      = c.getLong(0);
            s.title   = c.getString(1);
            s.lastMsg = c.getString(2);
            s.updated = c.getLong(3);
            data.add(s);
        }
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(inflater.inflate(R.layout.item_session, parent, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        final Session s = data.get(position);
        holder.title.setText(s.title);
        holder.msg.setText(s.lastMsg);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (clickListener != null) clickListener.onItemClick(s);
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                return longListener != null && longListener.onItemLongClick(s);
            }
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView title, msg;
        Holder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.item_title);
            msg   = (TextView) itemView.findViewById(R.id.item_msg);
        }
    }

    public void setOnItemClickListener(OnItemClickListener l) { clickListener = l; }
    public void setOnItemLongListener(OnItemLongListener l) { longListener = l; }
}

