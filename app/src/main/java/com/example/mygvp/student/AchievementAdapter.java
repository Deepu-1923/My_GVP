package com.example.mygvp.student;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygvp.R;
import com.example.mygvp.UploadAchievementActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    private Context context;
    private List<Achievement> achievementList;
    private boolean isGlobalView;
    private String currentUserId;

    public AchievementAdapter(Context context, List<Achievement> achievementList, boolean isGlobalView, String currentUserId) {
        this.context = context;
        this.achievementList = achievementList;
        this.isGlobalView = isGlobalView;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = achievementList.get(position);

        String title = achievement.getType();
        if (achievement.getCourseName() != null && !achievement.getCourseName().isEmpty()) {
            title += " - " + achievement.getCourseName();
        }
        holder.tvType.setText(title);
        holder.tvDomain.setText(achievement.getDomain());
        
        // Show "Certified: Name (Role)" instead of "Uploaded by..."
        String certInfo = "Certified: " + achievement.getUploaderName();
        certInfo += " (" + achievement.getUploaderType() + ")";
        certInfo += " on " + achievement.getDate();
        holder.tvDate.setText(certInfo);

        holder.btnView.setOnClickListener(v -> {
            if (achievement.getFileUrl() != null && !achievement.getFileUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(achievement.getFileUrl()));
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No certificate URL found", Toast.LENGTH_SHORT).show();
            }
        });

        // Only uploader can edit/delete their own
        if (achievement.getUploaderId() != null && achievement.getUploaderId().equals(currentUserId)) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        holder.btnDelete.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference("achievements")
                    .child(achievement.getId())
                    .removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show());
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (context instanceof UploadAchievementActivity) {
                ((UploadAchievementActivity) context).editAchievement(achievement);
            }
        });
    }

    @Override
    public int getItemCount() {
        return achievementList.size();
    }

    public static class AchievementViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDomain, tvDate;
        MaterialButton btnView;
        ImageButton btnEdit, btnDelete;
        ImageView ivMedal;

        public AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tv_ach_type);
            tvDomain = itemView.findViewById(R.id.tv_ach_domain);
            tvDate = itemView.findViewById(R.id.tv_ach_date);
            btnView = itemView.findViewById(R.id.btn_view_cert);
            btnEdit = itemView.findViewById(R.id.btn_edit_ach);
            btnDelete = itemView.findViewById(R.id.btn_delete_ach);
            ivMedal = itemView.findViewById(R.id.iv_medal);
        }
    }
}
