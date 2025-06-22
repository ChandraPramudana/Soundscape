package com.chandra.soundscape.doctor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chandra.soundscape.R;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class DoctorRecommendationAdapter extends RecyclerView.Adapter<DoctorRecommendationAdapter.ViewHolder> {

    private List<MusicTrack> recommendations;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MusicTrack music);
    }

    public DoctorRecommendationAdapter(List<MusicTrack> recommendations, OnItemClickListener listener) {
        this.recommendations = recommendations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicTrack music = recommendations.get(position);

        holder.tvTitle.setText(music.getTitle());
        holder.tvCategory.setText(music.getCategory());

        // Set status and color
        String status = music.getApprovalStatus();
        if ("pending".equals(status)) {
            holder.tvStatus.setText("Menunggu");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.status_pending));
        } else if ("approved".equals(status)) {
            holder.tvStatus.setText("Diterima");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.badge_approval_bg));
        } else if ("rejected".equals(status)) {
            holder.tvStatus.setText("Ditolak");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_red_dark));
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.badge_rejection_bg));

            // Show rejection reason if available
            if (music.getRejectionReason() != null && !music.getRejectionReason().isEmpty()) {
                holder.tvRejectionReason.setVisibility(View.VISIBLE);
                holder.tvRejectionReason.setText("Alasan: " + music.getRejectionReason());
            }
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(music);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvTitle;
        TextView tvCategory;
        TextView tvStatus;
        TextView tvRejectionReason;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvRejectionReason = itemView.findViewById(R.id.tv_rejection_reason);
        }
    }
}