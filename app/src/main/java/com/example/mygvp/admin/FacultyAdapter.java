package com.example.mygvp.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mygvp.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class FacultyAdapter extends RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder> {

    private Context context;
    private List<FacultyModel> facultyList;
    private OnFacultyClickListener onEditListener;
    private OnFacultyClickListener onDeleteListener;

    public interface OnFacultyClickListener {
        void onClick(FacultyModel faculty);
    }

    public FacultyAdapter(Context context, List<FacultyModel> facultyList, OnFacultyClickListener onEditListener, OnFacultyClickListener onDeleteListener) {
        this.context = context;
        this.facultyList = facultyList;
        this.onEditListener = onEditListener;
        this.onDeleteListener = onDeleteListener;
    }

    @NonNull
    @Override
    public FacultyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_faculty_manage, parent, false);
        return new FacultyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacultyViewHolder holder, int position) {
        FacultyModel faculty = facultyList.get(position);
        holder.tvName.setText(faculty.getName());
        holder.tvEmail.setText(faculty.getEmail());

        if (faculty.getImageUrl() != null && !faculty.getImageUrl().isEmpty()) {
            Glide.with(context).load(faculty.getImageUrl()).placeholder(R.drawable.ic_profile_placeholder).into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }

        holder.btnEdit.setOnClickListener(v -> onEditListener.onClick(faculty));
        holder.btnDelete.setOnClickListener(v -> onDeleteListener.onClick(faculty));
    }

    @Override
    public int getItemCount() {
        return facultyList.size();
    }

    public static class FacultyViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivProfile;
        TextView tvName, tvEmail;
        ImageButton btnEdit, btnDelete;

        public FacultyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivFacultyProfile);
            tvName = itemView.findViewById(R.id.tvFacultyName);
            tvEmail = itemView.findViewById(R.id.tvFacultyEmail);
            btnEdit = itemView.findViewById(R.id.btnEditFaculty);
            btnDelete = itemView.findViewById(R.id.btnDeleteFaculty);
        }
    }
}
