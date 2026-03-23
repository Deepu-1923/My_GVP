package com.example.mygvp.admin;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mygvp.R;
import com.google.android.material.chip.Chip;
import java.util.List;
import java.util.Locale;

public class StudentFeeAdapter extends RecyclerView.Adapter<StudentFeeAdapter.FeeViewHolder> {

    private List<StudentFee> feeList;

    public StudentFeeAdapter(List<StudentFee> feeList) {
        this.feeList = feeList;
    }

    @NonNull
    @Override
    public FeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_fee, parent, false);
        return new FeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeeViewHolder holder, int position) {
        StudentFee fee = feeList.get(position);
        holder.tvName.setText(fee.getName());
        holder.tvRoll.setText("Roll: " + fee.getRollNumber());
        holder.tvPaid.setText(String.format(Locale.getDefault(), "₹ %.2f", fee.getPaidAmount()));
        holder.tvDue.setText(String.format(Locale.getDefault(), "₹ %.2f", fee.getDueAmount()));

        if (fee.getDueAmount() <= 0) {
            holder.chipStatus.setText("Paid");
            holder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.bg_soft_green)));
            holder.chipStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
        } else {
            holder.chipStatus.setText("Pending");
            holder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.bg_soft_red)));
            holder.chipStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.error));
        }
    }

    @Override
    public int getItemCount() {
        return feeList.size();
    }

    public void updateList(List<StudentFee> newList) {
        this.feeList = newList;
        notifyDataSetChanged();
    }

    static class FeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRoll, tvPaid, tvDue;
        Chip chipStatus;

        public FeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvRoll = itemView.findViewById(R.id.tvRollNumber);
            tvPaid = itemView.findViewById(R.id.tvPaidAmount);
            tvDue = itemView.findViewById(R.id.tvDueAmount);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
    }
}
