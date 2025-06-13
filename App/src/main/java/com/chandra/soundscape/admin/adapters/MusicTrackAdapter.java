package com.chandra.soundscape.admin.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chandra.soundscape.FavoritesFragment;
import com.chandra.soundscape.MusicPlayerActivity;
import com.chandra.soundscape.R;
import com.chandra.soundscape.models.MusicTrack;
import com.bumptech.glide.Glide;
import java.util.List;

public class MusicTrackAdapter extends RecyclerView.Adapter<MusicTrackAdapter.ViewHolder> {
    private Context context;
    private List<MusicTrack> musicList;
    private boolean isHorizontal;
    private OnMusicClickListener listener;

    public interface OnMusicClickListener {
        void onMusicClick(MusicTrack music);
        void onPlayClick(MusicTrack music);
        void onFavoriteClick(MusicTrack music, boolean isFavorite);
    }

    public MusicTrackAdapter(Context context, List<MusicTrack> musicList, boolean isHorizontal) {
        this.context = context;
        this.musicList = musicList;
        this.isHorizontal = isHorizontal;
    }

    public void setOnMusicClickListener(OnMusicClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<MusicTrack> newList) {
        this.musicList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHorizontal ? R.layout.item_music_vertical : R.layout.item_music_horizontal;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view, isHorizontal);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicTrack track = musicList.get(position);

        // Set music title
        if (holder.tvMusicTitle != null) {
            holder.tvMusicTitle.setText(track.getTitle());
        }

        // Set artist
        if (holder.tvMusicArtist != null) {
            holder.tvMusicArtist.setText(track.getArtist());
        }

        // Set doctor name
        if (holder.tvDoctor != null) {
            if (track.getDoctorName() != null && !track.getDoctorName().isEmpty()) {
                holder.tvDoctor.setText("Dr. " + track.getDoctorName());
                if (holder.llDoctor != null) {
                    holder.llDoctor.setVisibility(View.VISIBLE);
                }
            } else {
                if (holder.llDoctor != null) {
                    holder.llDoctor.setVisibility(View.GONE);
                }
            }
        }

        // Set category
        if (holder.tvMusicCategory != null) {
            holder.tvMusicCategory.setText(track.getDisplayCategory());
        }

        // Set duration
        if (holder.tvMusicDuration != null) {
            holder.tvMusicDuration.setText(track.getDisplayDuration());
        }

        // Set rating (placeholder for now)
        if (holder.tvRating != null) {
            holder.tvRating.setText("4.5");
        }

        // Load music cover image
        if (holder.ivMusicCover != null) {
            if (track.hasImage()) {
                Glide.with(context)
                        .load(track.getImageUrl())
                        .placeholder(R.drawable.deepsleep1)
                        .error(R.drawable.deepsleep1)
                        .centerCrop()
                        .into(holder.ivMusicCover);
            } else {
                holder.ivMusicCover.setImageResource(R.drawable.deepsleep1);
            }
        }

        // Set premium badge visibility (if needed)
        if (holder.ivPremiumBadge != null) {
            holder.ivPremiumBadge.setVisibility(View.GONE); // or based on your logic
        }

        // Update favorite button state
        if (holder.ibFavorite != null) {
            boolean isFavorite = FavoritesFragment.isFavorite(context, track.getId());
            holder.ibFavorite.setImageResource(isFavorite ?
                    R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            if (isFavorite) {
                holder.ibFavorite.setColorFilter(context.getResources().getColor(R.color.colorError));
            } else {
                holder.ibFavorite.setColorFilter(context.getResources().getColor(R.color.icon_tint));
            }
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMusicClick(track);
            } else {
                // Default action - open MusicPlayerActivity
                Intent intent = new Intent(context, MusicPlayerActivity.class);
                intent.putExtra("music_track", track);
                context.startActivity(intent);
            }
        });

        // Play button click
        if (holder.btnPlay != null) {
            holder.btnPlay.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayClick(track);
                } else {
                    // Default action - open MusicPlayerActivity
                    Intent intent = new Intent(context, MusicPlayerActivity.class);
                    intent.putExtra("music_track", track);
                    context.startActivity(intent);
                }
            });
        }

        // Play overlay button click (for vertical layout)
        if (holder.btnPlayOverlay != null) {
            holder.btnPlayOverlay.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayClick(track);
                } else {
                    // Default action - open MusicPlayerActivity
                    Intent intent = new Intent(context, MusicPlayerActivity.class);
                    intent.putExtra("music_track", track);
                    context.startActivity(intent);
                }
            });
        }

        // Favorite button click
        if (holder.ibFavorite != null) {
            holder.ibFavorite.setOnClickListener(v -> {
                boolean isFavorite = FavoritesFragment.isFavorite(context, track.getId());

                if (isFavorite) {
                    // Remove from favorites
                    FavoritesFragment.removeFromFavorites(context, track.getId());
                    holder.ibFavorite.setImageResource(R.drawable.ic_favorite_border);
                    holder.ibFavorite.setColorFilter(context.getResources().getColor(R.color.icon_tint));
                    Toast.makeText(context, track.getTitle() + " dihapus dari favorit",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Add to favorites
                    FavoritesFragment.addToFavorites(context, track);
                    holder.ibFavorite.setImageResource(R.drawable.ic_favorite);
                    holder.ibFavorite.setColorFilter(context.getResources().getColor(R.color.colorError));
                    Toast.makeText(context, track.getTitle() + " ditambahkan ke favorit",
                            Toast.LENGTH_SHORT).show();
                }

                if (listener != null) {
                    listener.onFavoriteClick(track, !isFavorite);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return musicList != null ? musicList.size() : 0;
    }

    // Method to update favorite state of a specific item
    public void updateFavoriteState(String musicId) {
        for (int i = 0; i < musicList.size(); i++) {
            if (musicList.get(i).getId().equals(musicId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // Common views
        ImageView ivMusicCover, ivPremiumBadge;
        TextView tvMusicTitle, tvMusicArtist, tvDoctor, tvMusicCategory, tvMusicDuration, tvRating;
        ImageButton ibFavorite;
        Button btnPlay, btnPlayOverlay;
        LinearLayout llDoctor;

        ViewHolder(@NonNull View itemView, boolean isHorizontal) {
            super(itemView);

            // Initialize views based on layout
            ivMusicCover = itemView.findViewById(R.id.iv_music_cover);
            ivPremiumBadge = itemView.findViewById(R.id.iv_premium_badge);
            tvMusicTitle = itemView.findViewById(R.id.tv_music_title);
            tvMusicArtist = itemView.findViewById(R.id.tv_music_artist);
            tvDoctor = itemView.findViewById(R.id.tv_doctor);
            tvMusicCategory = itemView.findViewById(R.id.tv_music_category);
            tvMusicDuration = itemView.findViewById(R.id.tv_music_duration);
            tvRating = itemView.findViewById(R.id.tv_rating);
            ibFavorite = itemView.findViewById(R.id.ib_favorite);
            btnPlay = itemView.findViewById(R.id.btn_play);
            btnPlayOverlay = itemView.findViewById(R.id.btn_play_overlay);
            llDoctor = itemView.findViewById(R.id.ll_doctor);
        }
    }
}