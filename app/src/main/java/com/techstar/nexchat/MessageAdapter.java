package com.techstar.nexchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.techstar.nexchat.R;
import com.techstar.nexchat.model.Message;

import java.util.List;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;

public class MessageAdapter extends ArrayAdapter<Message> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT  = 1;

    MessageAdapter(Context ctx) {
        super(ctx, 0);
    }

    void replace(List<Message> list) {
        clear();
        addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() { return 2; }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).role.equals("user") ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Message msg = getItem(position);
        int type = getItemViewType(position);
        if (convertView == null) {
            int layout = (type == TYPE_USER)
                    ? R.layout.item_msg_right
                    : R.layout.item_msg_left;
            convertView = LayoutInflater.from(getContext()).inflate(layout, parent, false);
        }
        TextView tv = convertView.findViewById(R.id.tv_text);
        tv.setText(msg.content);
        return convertView;
    }
}
