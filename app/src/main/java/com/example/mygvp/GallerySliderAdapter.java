package com.example.mygvp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class GallerySliderAdapter extends RecyclerView.Adapter<GallerySliderAdapter.ViewHolder> {
    private List<Integer> images; // Changed from String to Integer for drawable IDs
    private List<String> titles;
    private Context context;

    public GallerySliderAdapter(Context context, List<Integer> images, List<String> titles) {
        this.context = context;
        this.images = images;
        this.titles = titles;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_gallery_full, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Loads the local drawable instantly
        Glide.with(context)
                .load(images.get(position))
                .into(holder.ivFullImage);

        // Set the title
        if (titles != null && position < titles.size()) {
            holder.tvImageTitle.setText(titles.get(position));
            holder.tvImageTitle.setVisibility(View.VISIBLE);
        } else {
            holder.tvImageTitle.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return images.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFullImage;
        TextView tvImageTitle;

        public ViewHolder(@NonNull View v) {
            super(v);
            ivFullImage = v.findViewById(R.id.ivFullImage);
            tvImageTitle = v.findViewById(R.id.tvImageTitle);
        }
    }
}