package com.example.solatnearby;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class HistoryAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<HashMap<String, String>> historyList;
    private OnHistoryActionListener listener;

    public interface OnHistoryActionListener {
        void onFavoriteClick(int position);
        void onCommentClick(int position);
        void onNavigateClick(int position);
        void onDeleteClick(int position);
    }

    public HistoryAdapter(Context context, ArrayList<HashMap<String, String>> historyList,
                          OnHistoryActionListener listener) {
        this.context = context;
        this.historyList = historyList;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return historyList.size();
    }

    @Override
    public Object getItem(int position) {
        return historyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView textMasjidName;
        TextView textAddress;
        TextView textDateTime;
        TextView textComment;
        ImageButton btnFavorite;
        Button btnNavigate;
        ImageButton btnDelete;
        View rootView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
            holder = new ViewHolder();
            holder.rootView = convertView;
            holder.textMasjidName = convertView.findViewById(R.id.textHistoryMasjidName);
            holder.textAddress = convertView.findViewById(R.id.textHistoryAddress);
            holder.textDateTime = convertView.findViewById(R.id.textHistoryDateTime);
            holder.textComment = convertView.findViewById(R.id.textHistoryComment);
            holder.btnFavorite = convertView.findViewById(R.id.btnFavorite);
            holder.btnNavigate = convertView.findViewById(R.id.btnNavigate);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HashMap<String, String> item = historyList.get(position);

        String masjidName = item.get("masjidName");
        String address = item.get("masjidAddress");
        String date = item.get("date");
        String time = item.get("time");
        String comment = item.get("userNote");
        String favorite = item.get("favorite");

        holder.textMasjidName.setText(masjidName);
        holder.textAddress.setText("📍 " + address);
        holder.textDateTime.setText("📅 " + date + "  •  🕐 " + time);

        // Comment
        if (comment != null && !comment.isEmpty()) {
            holder.textComment.setText("💬 " + comment);
        } else {
            holder.textComment.setText("💬 Tap to add your personal note");
        }

        //Favorite
        if ("true".equals(favorite)) {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
            holder.btnFavorite.setColorFilter(0xFFFF0000);
        } else {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
            holder.btnFavorite.setColorFilter(0xFF888888);
        }


        holder.btnFavorite.setOnClickListener(v -> {
            if (listener != null) listener.onFavoriteClick(position);
        });

        //Comment
        holder.textComment.setOnClickListener(v -> {
            if (listener != null) listener.onCommentClick(position);
        });

        //Navigate Button
        holder.btnNavigate.setOnClickListener(v -> {
            if (listener != null) listener.onNavigateClick(position);
        });

        //Delete
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(position);
        });

        return convertView;
    }
}