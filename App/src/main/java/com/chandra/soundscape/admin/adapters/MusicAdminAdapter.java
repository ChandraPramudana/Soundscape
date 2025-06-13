package com.chandra.soundscape.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.chandra.soundscape.R;
import com.chandra.soundscape.models.MusicTrack;
import java.util.List;

public class MusicAdminAdapter extends RecyclerView.Adapter<MusicAdminAdapter.MusicViewHolder> {
    private Context context;
    private List<MusicTrack> musicList;
    private OnMusicActionListener listener;

    public interface OnMusicActionListener {
        void onEditClick(MusicTrack music);
        void onDeleteClick(MusicTrack music);
        void onItemClick(MusicTrack music); // Add this for detail view
    }

    public MusicAdminAdapter(Context context, List<MusicTrack> musicList, OnMusicActionListener listener) {
        this.context = context;
        this.musicList = musicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_music_admin, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicTrack music = musicList.get(position);

        // Set basic info
        holder.tvTitle.setText(music.getTitle());
        holder.tvArtist.setText(music.getArtist() != null ? music.getArtist() : "Unknown Artist");
        holder.tvCategory.setText(music.getCategory() != null ? music.getCategory() : "Uncategorized");
        holder.tvDuration.setText(music.getDuration() != null ? music.getDuration() : "00:00");
        holder.tvPlayCount.setText(String.valueOf(music.getPlayCount()));

        // Load thumbnail
        if (music.getImageUrl() != null && !music.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(music.getImageUrl())
                    .placeholder(R.drawable.ic_default_soundscape)
                    .error(R.drawable.ic_default_soundscape)
                    .centerCrop()
                    .into(holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_default_soundscape);
        }

        // Doctor info
        if (music.getDoctorName() != null && !music.getDoctorName().isEmpty()) {
            holder.ivDoctor.setVisibility(View.VISIBLE);
            holder.tvDoctor.setVisibility(View.VISIBLE);
            holder.tvDoctor.setText(music.getDoctorName());
        } else {
            holder.ivDoctor.setVisibility(View.GONE);
            holder.tvDoctor.setVisibility(View.GONE);
        }

        // Journal info
        if (music.getJournalReference() != null && !music.getJournalReference().isEmpty()) {
            holder.ivJournal.setVisibility(View.VISIBLE);
        } else {
            holder.ivJournal.setVisibility(View.GONE);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(music);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(music);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(music);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public void updateData(List<MusicTrack> newList) {
        this.musicList = newList;
        notifyDataSetChanged();
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail, ivDoctor, ivJournal;
        TextView tvTitle, tvArtist, tvCategory, tvDuration, tvPlayCount, tvDoctor;
        ImageButton btnEdit, btnDelete;

        MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvPlayCount = itemView.findViewById(R.id.tv_play_count);
            ivDoctor = itemView.findViewById(R.id.iv_doctor);
            tvDoctor = itemView.findViewById(R.id.tv_doctor);
            ivJournal = itemView.findViewById(R.id.iv_journal);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}