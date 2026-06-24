package com.example.solatnearby;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MasjidAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<HashMap<String, String>> masjidList;
    private final HashMap<String, Bitmap> imageCache = new HashMap<>();

    public MasjidAdapter(Context context, ArrayList<HashMap<String, String>> masjidList) {
        this.context = context;
        this.masjidList = masjidList;
    }

    @Override
    public int getCount() {
        return masjidList.size();
    }

    @Override
    public Object getItem(int position) {
        return masjidList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView imageMasjidThumb;
        TextView textMasjidEmoji;
        TextView textMasjidName;
        TextView textMasjidAddress;
        TextView textMasjidDistance;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_masjid, parent, false);

            holder = new ViewHolder();
            holder.imageMasjidThumb = convertView.findViewById(R.id.imageMasjidThumb);
            holder.textMasjidEmoji = convertView.findViewById(R.id.textMasjidEmoji);
            holder.textMasjidName = convertView.findViewById(R.id.textMasjidName);
            holder.textMasjidAddress = convertView.findViewById(R.id.textMasjidAddress);
            holder.textMasjidDistance = convertView.findViewById(R.id.textMasjidDistance);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HashMap<String, String> masjid = masjidList.get(position);

        String name = masjid.get("name");
        String address = masjid.get("address");
        String distance = masjid.get("distance");
        String photoReference = masjid.get("photoReference");

        holder.textMasjidName.setText(name);
        holder.textMasjidAddress.setText(address);
        holder.textMasjidDistance.setText(distance);

        // Default fallback: show masjid emoji, hide image
        holder.imageMasjidThumb.setVisibility(View.GONE);
        holder.textMasjidEmoji.setVisibility(View.VISIBLE);
        holder.textMasjidEmoji.setText("🕌");

        if (photoReference != null && !photoReference.trim().isEmpty()) {
            holder.imageMasjidThumb.setTag(photoReference);
            loadMasjidPhoto(photoReference, holder.imageMasjidThumb, holder.textMasjidEmoji);
        }

        return convertView;
    }

    private void loadMasjidPhoto(String photoReference, ImageView imageView, TextView emojiView) {
        if (imageCache.containsKey(photoReference)) {
            imageView.setImageBitmap(imageCache.get(photoReference));
            imageView.setVisibility(View.VISIBLE);
            emojiView.setVisibility(View.GONE);
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String apiKey = context.getString(R.string.google_maps_key);

                String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                        + "?maxwidth=400"
                        + "&photo_reference=" + Uri.encode(photoReference)
                        + "&key=" + apiKey;

                URL url = new URL(photoUrl);

                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);

                InputStream inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap != null) {
                    imageCache.put(photoReference, bitmap);

                    ((Activity) context).runOnUiThread(() -> {
                        Object tag = imageView.getTag();

                        if (tag != null && tag.equals(photoReference)) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setVisibility(View.VISIBLE);
                            emojiView.setVisibility(View.GONE);
                        }
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("MASJID_PHOTO", "Nearby image failed: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}