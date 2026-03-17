package com.example.mygvp;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ContactInfoAdapter extends RecyclerView.Adapter<ContactInfoAdapter.ViewHolder> {

    private List<ContactInfo> infoList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String content);
    }

    public ContactInfoAdapter(List<ContactInfo> infoList, OnItemClickListener listener) {
        this.infoList = infoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactInfo item = infoList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvContent.setText(item.getContent());
        holder.ivIcon.setImageResource(item.getIconResId());

        // --- NEW: Click listeners for Maps, Email, Phone, and Website! ---
        holder.itemView.setOnClickListener(v -> {
            try {
                if (item.getTitle().contains("Address")) {
                    // Opens Google Maps to the exact college location
                    Uri gmmIntentUri = Uri.parse("geo:0,0?q=Gayatri+Vidya+Parishad+College+for+Degree+and+PG+Courses,+Rushikonda,+Visakhapatnam");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    v.getContext().startActivity(mapIntent);

                } else if (item.getTitle().contains("Email")) {
                    // Opens Email App
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:principalgvpcdpgca@gmail.com"));
                    v.getContext().startActivity(emailIntent);

                } else if (item.getTitle().contains("Contact")) {
                    // Opens Phone Dialer
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:08912783722"));
                    v.getContext().startActivity(dialIntent);
                } else if (item.getTitle().contains("Website") && listener != null) {
                    // Opens Website using the provided listener
                    listener.onItemClick(item.getContent());
                }
            } catch (Exception e) {
                // Prevents the app from crashing if the user doesn't have a map/email app installed
                Toast.makeText(v.getContext(), "App not found to perform this action", Toast.LENGTH_SHORT).show();
            }
        });
        // --------------------------------------------------------
    }

    @Override
    public int getItemCount() {
        return infoList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvInfoTitle);
            tvContent = itemView.findViewById(R.id.tvInfoContent);
            ivIcon = itemView.findViewById(R.id.ivInfoIcon);
        }
    }
}