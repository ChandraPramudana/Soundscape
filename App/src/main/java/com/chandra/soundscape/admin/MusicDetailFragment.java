package com.chandra.soundscape.admin;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.chandra.soundscape.R;
import com.chandra.soundscape.api.MusicApiClient;
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
import java.util.concurrent.TimeUnit;

public class MusicDetailFragment extends Fragment {
    private static final String TAG = "MusicDetailFragment";
    private static final String ARG_MUSIC_ID = "music_id";
    private static final String ARG_MUSIC = "music";

    // UI Components
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivCover;
    private TextView tvTitle, tvArtist, tvDuration, tvPlayCount;
    private TextView tvDoctorName, tvJournalReference, tvDescription;
    private TextView tvUploadDate, tvFileFormat, tvTotalPlays;
    private Chip chipCategory;
    private MaterialCardView cardDoctor, cardJournal, cardDescription, cardPlayer;
    private FloatingActionButton fabPlayPause;
    private ProgressBar progressBar, progressBarPlayer;
    private Toolbar toolbar;

    // Player components
    private TextView tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageView ivPlayPause;

    // Media Player
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isPlaying = false;
    private boolean isPrepared = false;

    // Data
    private MusicTrack musicTrack;
    private MusicApiClient musicApiClient;

    public static MusicDetailFragment newInstance(MusicTrack music) {
        MusicDetailFragment fragment = new MusicDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MUSIC, music);
        fragment.setArguments(args);
        return fragment;
    }

    public static MusicDetailFragment newInstance(int musicId) {
        MusicDetailFragment fragment = new MusicDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MUSIC_ID, musicId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        musicApiClient = MusicApiClient.getInstance();

        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_MUSIC)) {
                try {
                    musicTrack = (MusicTrack) getArguments().getSerializable(ARG_MUSIC);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing music track", e);
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_detail, container, false);

        try {
            initViews(view);
            setupToolbar();
            setupMediaPlayer();

            if (musicTrack != null) {
                displayMusicDetails();
            } else if (getArguments() != null && getArguments().containsKey(ARG_MUSIC_ID)) {
                loadMusicDetails(getArguments().getInt(ARG_MUSIC_ID));
            } else {
                Log.e(TAG, "No music data available");
                Toast.makeText(getContext(), "Data musik tidak ditemukan", Toast.LENGTH_SHORT).show();
                navigateBack();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(getContext(), "Terjadi kesalahan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void initViews(View view) {
        try {
            // Toolbar
            toolbar = view.findViewById(R.id.toolbar);
            collapsingToolbar = view.findViewById(R.id.collapsing_toolbar);

            // Cover image
            ivCover = view.findViewById(R.id.iv_cover);

            // Music info
            tvTitle = view.findViewById(R.id.tv_title);
            tvArtist = view.findViewById(R.id.tv_artist);
            tvDuration = view.findViewById(R.id.tv_duration);
            tvPlayCount = view.findViewById(R.id.tv_play_count);
            chipCategory = view.findViewById(R.id.chip_category);

            // Doctor card
            cardDoctor = view.findViewById(R.id.card_doctor);
            tvDoctorName = view.findViewById(R.id.tv_doctor_name);

            // Journal card
            cardJournal = view.findViewById(R.id.card_journal);
            tvJournalReference = view.findViewById(R.id.tv_journal_reference);

            // Description card
            cardDescription = view.findViewById(R.id.card_description);
            tvDescription = view.findViewById(R.id.tv_description);

            // Additional info
            tvUploadDate = view.findViewById(R.id.tv_upload_date);
            tvFileFormat = view.findViewById(R.id.tv_file_format);
            tvTotalPlays = view.findViewById(R.id.tv_total_plays);

            // Player card
            cardPlayer = view.findViewById(R.id.card_player);
            tvCurrentTime = view.findViewById(R.id.tv_current_time);
            tvTotalTime = view.findViewById(R.id.tv_total_time);
            seekBar = view.findViewById(R.id.seek_bar);
            ivPlayPause = view.findViewById(R.id.iv_play_pause);
            progressBarPlayer = view.findViewById(R.id.progress_bar_player);

            // FAB
            fabPlayPause = view.findViewById(R.id.fab_play_pause);
            if (fabPlayPause != null) {
                fabPlayPause.setOnClickListener(v -> togglePlayPause());
            }

            // Progress
            progressBar = view.findViewById(R.id.progress_bar);

            // Player controls
            if (ivPlayPause != null) {
                ivPlayPause.setOnClickListener(v -> togglePlayPause());
            }

            // SeekBar listener
            if (seekBar != null) {
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

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void setupToolbar() {
        try {
            if (getActivity() != null && toolbar != null) {
                ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
                if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                    ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar", e);
        }
    }

    private void setupMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            progressBarPlayer.setVisibility(View.GONE);

            // Set seekbar max value
            seekBar.setMax(mediaPlayer.getDuration());

            // Update total time
            updateTime(tvTotalTime, mediaPlayer.getDuration());

            // Start playing automatically
            startPlaying();
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            stopPlaying();
            seekBar.setProgress(0);
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
            progressBarPlayer.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Error memutar musik", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navigateBack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            // If no back stack, navigate to MusicListFragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MusicListFragment())
                    .commit();
        }
    }

    private void loadMusicDetails(int musicId) {
        showLoading(true);

        musicApiClient.getMusicById(musicId, new MusicApiClient.ApiCallback<MusicTrack>() {
            @Override
            public void onSuccess(MusicTrack result) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        musicTrack = result;
                        displayMusicDetails();
                        showLoading(false);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                        navigateBack();
                    });
                }
            }
        });
    }

    private void displayMusicDetails() {
        if (musicTrack == null || !isAdded()) return;

        try {
            // Set title
            if (collapsingToolbar != null && musicTrack.getTitle() != null) {
                collapsingToolbar.setTitle(musicTrack.getTitle());
            }

            if (tvTitle != null && musicTrack.getTitle() != null) {
                tvTitle.setText(musicTrack.getTitle());
            }

            // Set artist
            if (tvArtist != null) {
                if (musicTrack.getArtist() != null && !musicTrack.getArtist().isEmpty()) {
                    tvArtist.setText(musicTrack.getArtist());
                    tvArtist.setVisibility(View.VISIBLE);
                } else {
                    tvArtist.setVisibility(View.GONE);
                }
            }

            // Set category
            if (chipCategory != null && musicTrack.getCategory() != null) {
                chipCategory.setText(musicTrack.getCategory());
            }

            // Set duration
            if (tvDuration != null) {
                tvDuration.setText(musicTrack.getDuration() != null ? musicTrack.getDuration() : "00:00");
            }

            // Set play count
            if (tvPlayCount != null) {
                tvPlayCount.setText(musicTrack.getPlayCount() + " plays");
            }

            if (tvTotalPlays != null) {
                tvTotalPlays.setText(musicTrack.getPlayCount() + " kali");
            }

            // Load cover image
            if (ivCover != null && getContext() != null) {
                if (musicTrack.getImageUrl() != null && !musicTrack.getImageUrl().isEmpty()) {
                    Glide.with(this)
                            .load(musicTrack.getImageUrl())
                            .placeholder(R.drawable.ic_default_soundscape)
                            .error(R.drawable.ic_default_soundscape)
                            .centerCrop()
                            .into(ivCover);
                } else {
                    ivCover.setImageResource(R.drawable.ic_default_soundscape);
                }
            }

            // Doctor info
            if (cardDoctor != null && tvDoctorName != null) {
                if (musicTrack.getDoctorName() != null && !musicTrack.getDoctorName().isEmpty()) {
                    cardDoctor.setVisibility(View.VISIBLE);
                    tvDoctorName.setText(musicTrack.getDoctorName());
                } else {
                    cardDoctor.setVisibility(View.GONE);
                }
            }

            // Journal reference
            if (cardJournal != null && tvJournalReference != null) {
                if (musicTrack.getJournalReference() != null && !musicTrack.getJournalReference().isEmpty()) {
                    cardJournal.setVisibility(View.VISIBLE);
                    tvJournalReference.setText(musicTrack.getJournalReference());
                } else {
                    cardJournal.setVisibility(View.GONE);
                }
            }

            // Description
            if (cardDescription != null && tvDescription != null) {
                if (musicTrack.getDescription() != null && !musicTrack.getDescription().isEmpty()) {
                    cardDescription.setVisibility(View.VISIBLE);
                    tvDescription.setText(musicTrack.getDescription());
                } else {
                    cardDescription.setVisibility(View.GONE);
                }
            }

            // Additional info
            if (tvUploadDate != null && musicTrack.getCreatedAt() != null) {
                try {
                    String dateStr = formatDate(musicTrack.getCreatedAt());
                    tvUploadDate.setText(dateStr);
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting date", e);
                    tvUploadDate.setText(musicTrack.getCreatedAt());
                }
            }

            // File format
            if (tvFileFormat != null) {
                if (musicTrack.getAudioUrl() != null) {
                    String audioUrl = musicTrack.getAudioUrl().toLowerCase();
                    if (audioUrl.endsWith(".mp3")) {
                        tvFileFormat.setText("MP3");
                    } else if (audioUrl.endsWith(".wav")) {
                        tvFileFormat.setText("WAV");
                    } else if (audioUrl.endsWith(".m4a")) {
                        tvFileFormat.setText("M4A");
                    } else {
                        tvFileFormat.setText("Audio");
                    }
                } else {
                    tvFileFormat.setText("N/A");
                }
            }

            // Setup player if audio URL exists
            if (musicTrack.getAudioUrl() != null && !musicTrack.getAudioUrl().isEmpty()) {
                cardPlayer.setVisibility(View.VISIBLE);
                fabPlayPause.setVisibility(View.VISIBLE);
                prepareMusic();
            } else {
                cardPlayer.setVisibility(View.GONE);
                fabPlayPause.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error displaying music details", e);
            Toast.makeText(getContext(), "Error menampilkan detail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Log.e(TAG, "Unable to parse date: " + dateString, e2);
            }
        }

        return dateString;
    }

    private void prepareMusic() {
        if (musicTrack == null || musicTrack.getAudioUrl() == null) return;

        try {
            progressBarPlayer.setVisibility(View.VISIBLE);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(musicTrack.getAudioUrl());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Error preparing music", e);
            progressBarPlayer.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Error memuat musik", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (isPlaying) {
            pausePlaying();
        } else {
            if (isPrepared) {
                startPlaying();
            } else {
                prepareMusic();
            }
        }
    }

    private void startPlaying() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.start();
            isPlaying = true;
            updatePlayPauseButton();
            updateSeekBar();

            // Update play count
            updatePlayCount();
        }
    }

    private void pausePlaying() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            updatePlayPauseButton();
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.pause();
            }
            mediaPlayer.seekTo(0);
            isPlaying = false;
            updatePlayPauseButton();
            handler.removeCallbacks(updateSeekBarRunnable);
            seekBar.setProgress(0);
            updateTime(tvCurrentTime, 0);
        }
    }

    private void updatePlayPauseButton() {
        if (isPlaying) {
            ivPlayPause.setImageResource(R.drawable.ic_pause);
            fabPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            ivPlayPause.setImageResource(R.drawable.ic_play_arrow);
            fabPlayPause.setImageResource(R.drawable.ic_play_arrow);
        }
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
                updateTime(tvCurrentTime, currentPosition);
                handler.postDelayed(this, 100);
            }
        }
    };

    private void updateTime(TextView textView, int milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(minutes);
        textView.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
    }

    private void updatePlayCount() {
        // Increment play count locally
        if (musicTrack != null) {
            musicTrack.setPlayCount(musicTrack.getPlayCount() + 1);

            // Update UI
            if (tvPlayCount != null) {
                tvPlayCount.setText(musicTrack.getPlayCount() + " plays");
            }
            if (tvTotalPlays != null) {
                tvTotalPlays.setText(musicTrack.getPlayCount() + " kali");
            }

            // TODO: Update play count on server
            // musicApiClient.updatePlayCount(musicTrack.getId(), callback);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up media player
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
        musicTrack = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause music when fragment is paused
        if (isPlaying) {
            pausePlaying();
        }
    }
}