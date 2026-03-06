package com.example.mygvp;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.util.List;

public class GallerySliderAdapter extends RecyclerView.Adapter<GallerySliderAdapter.ViewHolder> {
    private List<String> images;
    private Context context;

    // BASED ON YOUR SCREENSHOT: The folder path is MyGVP/campus_gallery/
    // Replace 'your_cloud_name' with your actual Cloudinary name!
    private String baseUrl = "https://res.cloudinary.com/dlw4oisub/image/upload/f_auto,q_auto/MyGVP/campus_gallery/";

    public GallerySliderAdapter(Context context, List<String> images) {
        this.context = context;
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_gallery_full, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Cloudinary doesn't strictly need .jpg if you use f_auto, but it helps reliability
        String finalUrl = baseUrl + images.get(position) + ".jpg";

        holder.pbLoader.setVisibility(View.VISIBLE);

        Glide.with(context)
                .load(finalUrl)
                .placeholder(R.drawable.clg_img) // Shows your college pic while loading
                .error(R.drawable.clg_img)      // Fix: Uses clg_img if URL fails
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.pbLoader.setVisibility(View.GONE);
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.pbLoader.setVisibility(View.GONE);
                        return false;
                    }
                }).into(holder.ivFullImage);
    }

    @Override
    public int getItemCount() { return images.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFullImage;
        ProgressBar pbLoader;
        public ViewHolder(@NonNull View v) {
            super(v);
            ivFullImage = v.findViewById(R.id.ivFullImage);
            pbLoader = v.findViewById(R.id.pbLoader);
        }
    }
}