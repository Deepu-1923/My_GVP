package com.example.mygvp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    private List<GalleryItem> galleryItems;

    public GalleryAdapter(List<GalleryItem> galleryItems) {
        this.galleryItems = galleryItems;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_card, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        GalleryItem item = galleryItems.get(position);
        holder.tvTitle.setText(item.getTitle());

        int resId = holder.itemView.getContext().getResources().getIdentifier(
                item.getImagePath(), "drawable", holder.itemView.getContext().getPackageName());
        
        if (resId != 0) {
            holder.ivImage.setImageResource(resId);
        } else {
            holder.ivImage.setImageResource(R.drawable.gvplogo);
        }
    }

    @Override
    public int getItemCount() {
        return galleryItems.size();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivGalleryImage);
            tvTitle = itemView.findViewById(R.id.tvGalleryTitle);
        }
    }
}
