package com.chandra.soundscape.doctor;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.chandra.soundscape.R;
import com.chandra.soundscape.models.MusicTrack;

public class DoctorRecommendationDetailDialog extends DialogFragment {

    private MusicTrack music;

    public DoctorRecommendationDetailDialog(MusicTrack music) {
        this.music = music;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_recommendation_detail, null);

        // Set data
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvArtist = view.findViewById(R.id.tv_artist);
        TextView tvCategory = view.findViewById(R.id.tv_category);
        TextView tvDuration = view.findViewById(R.id.tv_duration);
        TextView tvDoctor = view.findViewById(R.id.tv_doctor);
        TextView tvJournal = view.findViewById(R.id.tv_journal);
        TextView tvDescription = view.findViewById(R.id.tv_description);
        TextView tvStatus = view.findViewById(R.id.tv_status);
        TextView tvRejectionReason = view.findViewById(R.id.tv_rejection_reason);

        tvTitle.setText(music.getTitle());
        tvArtist.setText(music.getArtist());
        tvCategory.setText(music.getCategory());
        tvDuration.setText(music.getDuration());
        tvDoctor.setText(music.getDoctorName());
        tvJournal.setText(music.getJournalReference());
        tvDescription.setText(music.getDescription());

        // Status
        String status = music.getApprovalStatus();
        if ("pending".equals(status)) {
            tvStatus.setText("Status: Menunggu Persetujuan");
            tvStatus.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark));
        } else if ("approved".equals(status)) {
            tvStatus.setText("Status: Diterima");
            tvStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark));
        } else if ("rejected".equals(status)) {
            tvStatus.setText("Status: Ditolak");
            tvStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));

            if (music.getRejectionReason() != null && !music.getRejectionReason().isEmpty()) {
                tvRejectionReason.setVisibility(View.VISIBLE);
                tvRejectionReason.setText("Alasan penolakan: " + music.getRejectionReason());
            }
        }

        builder.setView(view)
                .setTitle("Detail Rekomendasi")
                .setPositiveButton("Tutup", null);

        return builder.create();
    }
}