package com.example.mygvp.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private final Context context;
    private final List<StudentModel> studentList;
    private final OnStudentClickListener onEditListener;
    private final OnStudentClickListener onDeleteListener;

    public interface OnStudentClickListener {
        void onClick(StudentModel student);
    }

    public StudentAdapter(Context context, List<StudentModel> studentList, OnStudentClickListener onEditListener, OnStudentClickListener onDeleteListener) {
        this.context = context;
        this.studentList = studentList;
        this.onEditListener = onEditListener;
        this.onDeleteListener = onDeleteListener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student_manage, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentModel student = studentList.get(position);
        holder.tvName.setText(student.getName());
        holder.tvRoll.setText(student.getRollNo());
        holder.tvEmail.setText(student.getEmail());

        // Construct Cloudinary URL based on path: students/Branch/Batch/RollNo
        String cloudName = "dlw4oisub";
        String branch = student.getBranch();
        String batch = student.getBatch();
        String roll = student.getRollNo();

        // Manual URL with .jpg extension (essential for direct delivery) and face detection
        String cloudinaryUrl = "https://res.cloudinary.com/" + cloudName + "/image/upload/w_200,h_200,c_fill,g_face,q_auto,f_auto/students/" + branch + "/" + batch + "/" + roll + ".jpg";

        Glide.with(context)
                .load(cloudinaryUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(holder.ivProfile);

        holder.btnEdit.setOnClickListener(v -> onEditListener.onClick(student));
        holder.btnDelete.setOnClickListener(v -> onDeleteListener.onClick(student));
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivProfile;
        TextView tvName, tvEmail, tvRoll;
        MaterialButton btnEdit, btnDelete;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivStudentProfile);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvRoll = itemView.findViewById(R.id.tvStudentRoll);
            tvEmail = itemView.findViewById(R.id.tvStudentEmail);
            btnEdit = itemView.findViewById(R.id.btnEditStudent);
            btnDelete = itemView.findViewById(R.id.btnDeleteStudent);
        }
    }
}
