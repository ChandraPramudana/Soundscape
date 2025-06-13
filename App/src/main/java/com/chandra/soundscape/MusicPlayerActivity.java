package com.chandra.soundscape;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import com.bumptech.glide.Glide;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MusicPlayerActivity extends AppCompatActivity {
    // UI Components - Main
    private CollapsingToolbarLayout collapsingToolbar;
    private Toolbar toolbar;
    private ImageView ivAlbumArtBg, ivFavorite;
    private TextView tvMusicTitle, tvMusicArtist, tvCurrentTime, tvTotalTime;
    private TextView tvDuration, tvPlayCount;
    private SeekBar seekBar;
    private ImageButton btnPrevious, btnNext;
    private FloatingActionButton btnPlayPause;
    private ProgressBar loadingProgress;
    private Chip chipCategory;

    // UI Components - Detail Cards
    private MaterialCardView cardDoctor, cardJournal, cardDescription, cardRecommendation;
    private TextView tvDoctorName, tvJournalReference, tvDescription, tvRecommendationText;

    // UI Components - Additional Info
    private LinearLayout llCreatedDate, llUpdatedDate, llCreatedBy;
    private TextView tvCreatedDate, tvUpdatedDate, tvCreatedBy;

    // Media Player
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isPlaying = false;
    private boolean isPrepared = false;

    // Data
    private MusicTrack currentTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        // Get music data from intent
        currentTrack = (MusicTrack) getIntent().getSerializableExtra("music_track");
        if (currentTrack == null) {
            Toast.makeText(this, "Error: No music data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupMediaPlayer();
        updateUI();
        displayMusicDetails();
    }

    private void initViews() {
        // Toolbar and AppBar
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);

        // Main components
        ivAlbumArtBg = findViewById(R.id.iv_album_art_bg);
        ivFavorite = findViewById(R.id.iv_favorite);
        tvMusicTitle = findViewById(R.id.tv_music_title);
        tvMusicArtist = findViewById(R.id.tv_music_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        tvDuration = findViewById(R.id.tv_duration);
        tvPlayCount = findViewById(R.id.tv_play_count);
        seekBar = findViewById(R.id.seekbar_music);
        btnPrevious = findViewById(R.id.btn_previous);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);
        loadingProgress = findViewById(R.id.progress_bar);
        chipCategory = findViewById(R.id.chip_category);

        // Detail cards
        cardDoctor = findViewById(R.id.card_doctor);
        cardJournal = findViewById(R.id.card_journal);
        cardDescription = findViewById(R.id.card_description);
        cardRecommendation = findViewById(R.id.card_recommendation);

        // Detail TextViews
        tvDoctorName = findViewById(R.id.tv_doctor_name);
        tvJournalReference = findViewById(R.id.tv_journal_reference);
        tvDescription = findViewById(R.id.tv_description);
        tvRecommendationText = findViewById(R.id.tv_recommendation_text);

        // Additional info
        llCreatedDate = findViewById(R.id.ll_created_date);
        llUpdatedDate = findViewById(R.id.ll_updated_date);
        llCreatedBy = findViewById(R.id.ll_created_by);
        tvCreatedDate = findViewById(R.id.tv_created_date);
        tvUpdatedDate = findViewById(R.id.tv_updated_date);
        tvCreatedBy = findViewById(R.id.tv_created_by);

        // Set click listeners
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        ivFavorite.setOnClickListener(v -> toggleFavorite());

        // SeekBar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && isPrepared) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void updateUI() {
        // Update title and artist
        if (collapsingToolbar != null && currentTrack.getTitle() != null) {
            collapsingToolbar.setTitle(currentTrack.getTitle());
        }

        tvMusicTitle.setText(currentTrack.getTitle());
        tvMusicArtist.setText(currentTrack.getArtist());

        // Load album art
        if (currentTrack.hasImage()) {
            Glide.with(this)
                    .load(currentTrack.getImageUrl())
                    .placeholder(R.drawable.deepsleep1)
                    .error(R.drawable.deepsleep1)
                    .centerCrop()
                    .into(ivAlbumArtBg);
        }
    }

    private void displayMusicDetails() {
        // Category
        if (chipCategory != null && currentTrack.getCategory() != null) {
            chipCategory.setText(currentTrack.getCategory());
        }

        // Duration
        if (tvDuration != null) {
            tvDuration.setText(currentTrack.getDuration() != null ? currentTrack.getDuration() : "00:00");
        }

        // Play count
        if (tvPlayCount != null) {
            tvPlayCount.setText(currentTrack.getPlayCount() + " plays");
        }

        // Doctor info
        if (currentTrack.getDoctorName() != null && !currentTrack.getDoctorName().isEmpty()) {
            cardDoctor.setVisibility(View.VISIBLE);
            tvDoctorName.setText("Dr. " + currentTrack.getDoctorName());
        } else {
            cardDoctor.setVisibility(View.GONE);
        }

        // Journal reference
        if (currentTrack.getJournalReference() != null && !currentTrack.getJournalReference().isEmpty()) {
            cardJournal.setVisibility(View.VISIBLE);
            tvJournalReference.setText(currentTrack.getJournalReference());
        } else {
            cardJournal.setVisibility(View.GONE);
        }

        // Description
        if (currentTrack.getDescription() != null && !currentTrack.getDescription().isEmpty()) {
            cardDescription.setVisibility(View.VISIBLE);
            tvDescription.setText(currentTrack.getDescription());
        } else {
            cardDescription.setVisibility(View.GONE);
        }

        // Doctor Recommendation Text
        if (currentTrack.getRecommendationText() != null && !currentTrack.getRecommendationText().isEmpty()) {
            cardRecommendation.setVisibility(View.VISIBLE);
            tvRecommendationText.setText(currentTrack.getRecommendationText());
        } else {
            cardRecommendation.setVisibility(View.GONE);
        }

        // Additional info
        if (currentTrack.getCreatedAt() != null && !currentTrack.getCreatedAt().isEmpty()) {
            llCreatedDate.setVisibility(View.VISIBLE);
            tvCreatedDate.setText(formatDate(currentTrack.getCreatedAt()));
        } else {
            llCreatedDate.setVisibility(View.GONE);
        }

        if (currentTrack.getUpdatedAt() != null && !currentTrack.getUpdatedAt().isEmpty()) {
            llUpdatedDate.setVisibility(View.VISIBLE);
            tvUpdatedDate.setText(formatDate(currentTrack.getUpdatedAt()));
        } else {
            llUpdatedDate.setVisibility(View.GONE);
        }

        if (currentTrack.getCreatedBy() != null && !currentTrack.getCreatedBy().isEmpty()) {
            llCreatedBy.setVisibility(View.VISIBLE);
            tvCreatedBy.setText(currentTrack.getCreatedBy());
        } else {
            llCreatedBy.setVisibility(View.GONE);
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "Tanggal tidak tersedia";
        }

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = isoFormat.parse(dateString);

            if (date != null) {
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
                return displayFormat.format(date);
            }
        } catch (ParseException e) {
            try {
                SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                Date date = altFormat.parse(dateString);

                if (date != null) {
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
                    return displayFormat.format(date);
                }
            } catch (ParseException e2) {
                e2.printStackTrace();
            }
        }

        return dateString;
    }

    private void setupMediaPlayer() {
        if (currentTrack.getAudioUrl() == null || currentTrack.getAudioUrl().isEmpty()) {
            Toast.makeText(this, "Error: No audio URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(currentTrack.getAudioUrl());
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                showLoading(false);
                int duration = mp.getDuration();
                seekBar.setMax(duration);
                tvTotalTime.setText(formatTime(duration));
                // Auto play when ready
                togglePlayPause();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
                seekBar.setProgress(0);
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                showLoading(false);
                Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
                return false;
            });

        } catch (IOException e) {
            showLoading(false);
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;

        if (isPlaying) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
            isPlaying = false;
            handler.removeCallbacks(updateSeekBarRunnable);
        } else {
            if (isPrepared) {
                mediaPlayer.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                isPlaying = true;
                updateSeekBar();
                updatePlayCount();
            }
        }
    }

    private void toggleFavorite() {
        // TODO: Implement favorite functionality
        Toast.makeText(this, "Favorite clicked", Toast.LENGTH_SHORT).show();
    }

    private void updateSeekBar() {
        handler.postDelayed(updateSeekBarRunnable, 100);
    }

    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                tvCurrentTime.setText(formatTime(currentPosition));
                handler.postDelayed(this, 100);
            }
        }
    };

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void updatePlayCount() {
        // Increment play count locally
        if (currentTrack != null) {
            currentTrack.setPlayCount(currentTrack.getPlayCount() + 1);

            // Update UI
            if (tvPlayCount != null) {
                tvPlayCount.setText(currentTrack.getPlayCount() + " plays");
            }

            // TODO: Update play count on server
            // You can call your API here to update the play count
        }
    }

    private void showLoading(boolean show) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnPlayPause.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.stop();
        }
    }
}