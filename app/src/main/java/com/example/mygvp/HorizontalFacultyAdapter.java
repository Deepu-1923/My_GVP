package com.example.mygvp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HorizontalFacultyAdapter extends RecyclerView.Adapter<HorizontalFacultyAdapter.ViewHolder> {

    private List<Faculty> facultyList;

    public HorizontalFacultyAdapter(List<Faculty> facultyList) {
        this.facultyList = facultyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faculty_card_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Faculty faculty = facultyList.get(position % facultyList.size());
        holder.tvName.setText(faculty.getName());
        holder.tvDesignation.setText(faculty.getDesignation());

        int resId = holder.itemView.getContext().getResources().getIdentifier(
                faculty.getImagePath(), "drawable", holder.itemView.getContext().getPackageName());
        
        if (resId != 0) {
            holder.ivPhoto.setImageResource(resId);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE; // For infinite loop effect
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvDesignation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivFacultyPhoto);
            tvName = itemView.findViewById(R.id.tvFacultyName);
            tvDesignation = itemView.findViewById(R.id.tvFacultyDesignation);
        }
    }
}