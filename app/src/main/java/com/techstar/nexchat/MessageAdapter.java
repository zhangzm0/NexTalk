package com.techstar.nexchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.techstar.nexchat.model.Message;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;

public class MessageAdapter extends ArrayAdapter<Message> {

    private final Markwon markwon;

    public MessageAdapter(Context ctx, Markwon markwon) {
        super(ctx, 0);
        this.markwon = markwon;
    }

    public void replace(List<Message> list) {
        clear();
        addAll(list);
        notifyDataSetChanged();
    }

    @Override
	public int getViewTypeCount() { return 2; }

	@Override
	public int getItemViewType(int position) {
		return "user".equals(getItem(position).role) ? 1 : 0;
	}

	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		int type = getItemViewType(pos);
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext())
                .inflate(type == 1 ? R.layout.item_msg_right : R.layout.item_msg_left,
                         parent, false);
		}
		TextView tv = (TextView) convertView.findViewById(R.id.tv_text);
		markwon.setMarkdown(tv, getItem(pos).content);
		
		return convertView;
	}
	

    private static class ViewHolder {
        TextView text;
    }
}

