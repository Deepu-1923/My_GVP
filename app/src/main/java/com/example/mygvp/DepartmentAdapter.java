package com.example.mygvp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {

    private final List<String> departments;
    private final OnDepartmentClickListener listener;

    public interface OnDepartmentClickListener {
        void onDepartmentClick(String department);
    }

    public DepartmentAdapter(List<String> departments, OnDepartmentClickListener listener) {
        this.departments = departments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_department_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String dept = departments.get(position);
        holder.tvDeptName.setText(dept);
        updateIcon(holder, dept);
        holder.itemView.setOnClickListener(v -> listener.onDepartmentClick(dept));
    }

    @Override
    public int getItemCount() {
        return departments.size();
    }

    private void updateIcon(ViewHolder holder, String dept) {
        int iconResId;
        int colorResId;

        switch (dept) {
            case "CSE":
                iconResId = R.drawable.ic_cse_engg;
                colorResId = R.color.icon_purple;
                break;
            case "CSM":
                iconResId = R.drawable.ic_csm_engg;
                colorResId = R.color.icon_blue;
                break;
            case "Civil":
                iconResId = R.drawable.ic_civil_engg;
                colorResId = R.color.icon_orange;
                break;
            case "ECE":
                iconResId = R.drawable.ic_electrical_engg;
                colorResId = R.color.icon_green;
                break;
            case "Mech":
                iconResId = R.drawable.ic_mechanical_engg;
                colorResId = R.color.icon_red;
                break;
            default:
                iconResId = R.drawable.ic_info;
                colorResId = R.color.textSecondary;
        }

        holder.ivDeptIcon.setImageResource(iconResId);
        holder.ivDeptIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(colorResId));
        holder.flDeptIconBg.getBackground().setTint(holder.itemView.getContext().getResources().getColor(colorResId));
        holder.flDeptIconBg.getBackground().setAlpha(25);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeptName;
        ImageView ivDeptIcon;
        android.view.View flDeptIconBg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeptName = itemView.findViewById(R.id.tvDeptName);
            ivDeptIcon = itemView.findViewById(R.id.ivDeptIcon);
            flDeptIconBg = itemView.findViewById(R.id.flDeptIconBg);
        }
    }
}
