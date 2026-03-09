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
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FacultyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_FACULTY = 1;

    private Context context;
    private List<Object> items;

    public FacultyAdapter(Context context, List<Object> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return TYPE_HEADER;
        } else {
            return TYPE_FACULTY;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_faculty_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_faculty_card, parent, false);
            return new FacultyViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            String branchName = (String) items.get(position);
            ((HeaderViewHolder) holder).tvHeader.setText(branchName);
        } else {
            Faculty faculty = (Faculty) items.get(position);
            FacultyViewHolder fHolder = (FacultyViewHolder) holder;

            fHolder.tvName.setText(faculty.getName() != null ? faculty.getName() : "N/A");
            fHolder.tvQual.setText(faculty.getQualification() != null ? faculty.getQualification() : "N/A");
            fHolder.tvSpec.setText(faculty.getSpecialization() != null ? faculty.getSpecialization() : "N/A");
            fHolder.tvEmail.setText(faculty.getEmail() != null ? faculty.getEmail() : "N/A");

            // Load Cloudinary image using Glide
            if (faculty.getImageUrl() != null && !faculty.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(faculty.getImageUrl())
                        .placeholder(R.drawable.ic_profile_placeholder) // Keep your placeholder
                        .into(fHolder.ivImage);
            } else {
                fHolder.ivImage.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvBranchHeader);
        }
    }

    static class FacultyViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvQual, tvSpec;
        MaterialButton tvEmail;

        FacultyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivFacultyImage);
            tvName = itemView.findViewById(R.id.tvFacName);
            tvQual = itemView.findViewById(R.id.tvFacQual);
            tvSpec = itemView.findViewById(R.id.tvFacSpec);
            tvEmail = itemView.findViewById(R.id.tvFacEmail);
        }
    }
}