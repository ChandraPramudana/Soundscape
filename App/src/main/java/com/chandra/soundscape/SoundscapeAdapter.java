package com.chandra.soundscape;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class SoundscapeAdapter extends RecyclerView.Adapter<SoundscapeAdapter.ViewHolder> {
    private Context context;
    private List<Soundscape> soundscapes;
    private boolean isHorizontal;

    public SoundscapeAdapter(Context context, List<Soundscape> soundscapes, boolean isHorizontal) {
        this.context = context;
        this.soundscapes = soundscapes;
        this.isHorizontal = isHorizontal;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHorizontal ? R.layout.item_soundscape_horizontal : R.layout.item_soundscape_vertical;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Soundscape soundscape = soundscapes.get(position);
        holder.bind(soundscape);
    }

    @Override
    public int getItemCount() {
        return soundscapes.size();
    }

    public void updateData(List<Soundscape> newSoundscapes) {
        this.soundscapes = newSoundscapes;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail, favorite;
        TextView title, creator, category, duration, rating;
        MaterialButton playButton;
        ImageButton moreButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.iv_thumbnail);
            favorite = itemView.findViewById(R.id.iv_favorite);
            title = itemView.findViewById(R.id.tv_title);
            creator = itemView.findViewById(R.id.tv_creator);
            category = itemView.findViewById(R.id.tv_category);
            duration = itemView.findViewById(R.id.tv_duration);
            rating = itemView.findViewById(R.id.tv_rating);
            playButton = itemView.findViewById(R.id.btn_play);
            moreButton = itemView.findViewById(R.id.btn_more);
        }

        void bind(Soundscape soundscape) {
            thumbnail.setImageResource(soundscape.getThumbnailResId());
            title.setText(soundscape.getTitle());
            creator.setText("oleh " + soundscape.getCreator());
            category.setText(soundscape.getCategory());
            duration.setText(soundscape.getDuration());
            rating.setText(String.valueOf(soundscape.getRating()));

            favorite.setImageResource(soundscape.isFavorite() ?
                    R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            // Set click listeners
            playButton.setOnClickListener(v -> {
                // Handle play action
            });

            favorite.setOnClickListener(v -> {
                soundscape.setFavorite(!soundscape.isFavorite());
                favorite.setImageResource(soundscape.isFavorite() ?
                        R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            });
        }
    }
}
