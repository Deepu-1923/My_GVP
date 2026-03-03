package com.example.mygvp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class FacultyAdapter extends RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder> {

    private List<Faculty> facultyList;

    public FacultyAdapter(List<Faculty> facultyList) {
        this.facultyList = facultyList;
    }

    @NonNull
    @Override
    public FacultyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faculty_card, parent, false);
        return new FacultyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacultyViewHolder holder, int position) {
        Faculty faculty = facultyList.get(position);
        holder.tvName.setText(faculty.getName());
        holder.tvDesignation.setText(faculty.getDesignation());
        holder.tvEmail.setText(faculty.getEmail());

        // Assuming images are in drawables with names matching imagePath
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
        return facultyList.size();
    }

    static class FacultyViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvDesignation, tvEmail;

        public FacultyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivFacultyPhoto);
            tvName = itemView.findViewById(R.id.tvFacultyName);
            tvDesignation = itemView.findViewById(R.id.tvFacultyDesignation);
            tvEmail = itemView.findViewById(R.id.tvFacultyEmail);
        }
    }
}
