package com.example.mygvp.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygvp.R;

import java.util.List;

public class MonthlyAttendanceAdapter extends RecyclerView.Adapter<MonthlyAttendanceAdapter.ViewHolder> {

    private List<MonthlyAttendance> attendanceList;

    public MonthlyAttendanceAdapter(List<MonthlyAttendance> attendanceList) {
        this.attendanceList = attendanceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monthly_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonthlyAttendance item = attendanceList.get(position);
        holder.tvMonthName.setText(item.getMonthName());
        holder.tvDaysInfo.setText("Present: " + item.getPresent() + " / Total: " + item.getTotal());
        holder.tvMonthPercentage.setText(item.getPercentage());
    }

    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthName, tvDaysInfo, tvMonthPercentage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonthName = itemView.findViewById(R.id.tvMonthName);
            tvDaysInfo = itemView.findViewById(R.id.tvDaysInfo);
            tvMonthPercentage = itemView.findViewById(R.id.tvMonthPercentage);
        }
    }
}