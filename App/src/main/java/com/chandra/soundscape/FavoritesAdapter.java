package com.chandra.soundscape;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private Context context;
    private List<MusicTrack> favorites;
    private OnFavoriteClickListener listener;

    public interface OnFavoriteClickListener {
        void onPlayClick(MusicTrack musicTrack);
        void onFavoriteToggle(MusicTrack musicTrack, int position);
        void onItemClick(MusicTrack musicTrack);
    }

    public FavoritesAdapter(Context context, List<MusicTrack> favorites) {
        this.context = context;
        this.favorites = favorites;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite_soundscape, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        MusicTrack musicTrack = favorites.get(position);

        // Set music title
        holder.tvTitle.setText(musicTrack.getTitle());

        // Set artist
        holder.tvArtist.setText(musicTrack.getArtist());

        // Set category
        holder.tvCategory.setText(musicTrack.getDisplayCategory());

        // Set duration
        holder.tvDuration.setText(musicTrack.getDisplayDuration());

        // Set play count
        holder.tvPlayCount.setText(String.valueOf(musicTrack.getPlayCount()));

        // Set rating (placeholder - you can implement actual rating system)
        holder.tvRating.setText("4.5");

        // Load cover image
        if (musicTrack.hasImage()) {
            Glide.with(context)
                    .load(musicTrack.getImageUrl())
                    .placeholder(R.drawable.deepsleep1)
                    .error(R.drawable.deepsleep1)
                    .centerCrop()
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.deepsleep1);
        }

        // Set doctor info
        if (musicTrack.hasDoctor()) {
            holder.llDoctor.setVisibility(View.VISIBLE);
            holder.tvDoctor.setText("Dr. " + musicTrack.getDoctorName());
        } else {
            holder.llDoctor.setVisibility(View.GONE);
        }

        // Set premium badge visibility
        holder.ivPremiumBadge.setVisibility(View.GONE); // You can add premium logic here

        // Click listeners
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(musicTrack);
            }
        });

        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayClick(musicTrack);
            }
        });

        holder.ibFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteToggle(musicTrack, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < favorites.size()) {
            favorites.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, favorites.size());
        }
    }

    public void updateData(List<MusicTrack> newFavorites) {
        this.favorites = newFavorites;
        notifyDataSetChanged();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivCover, ivPremiumBadge;
        TextView tvTitle, tvArtist, tvCategory, tvRating, tvDuration, tvPlayCount, tvDoctor;
        FloatingActionButton btnPlay;
        ImageButton ibFavorite;
        LinearLayout llDoctor;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_music);
            ivCover = itemView.findViewById(R.id.iv_music_cover);
            ivPremiumBadge = itemView.findViewById(R.id.iv_premium_badge);
            tvTitle = itemView.findViewById(R.id.tv_music_title);
            tvArtist = itemView.findViewById(R.id.tv_music_artist);
            tvCategory = itemView.findViewById(R.id.tv_music_category);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvDuration = itemView.findViewById(R.id.tv_music_duration);
            tvPlayCount = itemView.findViewById(R.id.tv_play_count);
            tvDoctor = itemView.findViewById(R.id.tv_doctor);
            btnPlay = itemView.findViewById(R.id.btn_play_overlay);
            ibFavorite = itemView.findViewById(R.id.ib_favorite);
            llDoctor = itemView.findViewById(R.id.ll_doctor);
        }
    }
}