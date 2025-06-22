package com.chandra.soundscape.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chandra.soundscape.R;
import com.google.android.material.card.MaterialCardView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.ViewHolder> {

    private List<DoctorListFragment.Doctor> doctorList;
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));

    public DoctorAdapter(List<DoctorListFragment.Doctor> doctorList) {
        this.doctorList = doctorList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DoctorListFragment.Doctor doctor = doctorList.get(position);

        // Set nama
        holder.tvName.setText(doctor.getName());

        // Set email
        holder.tvEmail.setText(doctor.getEmail());

        // Format dan set tanggal bergabung
        String joinDate = formatDate(doctor.getJoinDate());
        holder.tvJoinDate.setText("Bergabung: " + joinDate);

        // Format dan set last active dengan status
        String lastActiveText = formatLastActive(doctor.getLastActive());
        holder.tvLastActive.setText(lastActiveText);

        // Set warna berdasarkan aktivitas terakhir
        if (isRecentlyActive(doctor.getLastActive())) {
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.white));
        } else {
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return doctorList != null ? doctorList.size() : 0;
    }

    // Helper method untuk format tanggal
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "-";
        }

        try {
            Date date = inputFormat.parse(dateStr.replace("Z", ""));
            return outputFormat.format(date);
        } catch (ParseException e) {
            // Try alternative formats
            try {
                SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = altFormat.parse(dateStr);
                return outputFormat.format(date);
            } catch (ParseException e2) {
                return dateStr; // Return original if parsing fails
            }
        }
    }

    // Helper method untuk format last active
    private String formatLastActive(String lastActiveStr) {
        if (lastActiveStr == null || lastActiveStr.isEmpty()) {
            return "Terakhir aktif: Belum pernah";
        }

        try {
            Date lastActive = inputFormat.parse(lastActiveStr.replace("Z", ""));
            Date now = new Date();

            long diffInMillis = now.getTime() - lastActive.getTime();
            long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);

            String status;
            if (minutes < 60) {
                status = minutes + " menit yang lalu";
            } else if (hours < 24) {
                status = hours + " jam yang lalu";
            } else if (days < 7) {
                status = days + " hari yang lalu";
            } else if (days < 30) {
                long weeks = days / 7;
                status = weeks + " minggu yang lalu";
            } else {
                // Tampilkan tanggal lengkap jika lebih dari 30 hari
                status = outputFormat.format(lastActive) + " " + timeFormat.format(lastActive);
            }

            return "Terakhir aktif: " + status;

        } catch (ParseException e) {
            return "Terakhir aktif: " + lastActiveStr;
        }
    }

    // Helper method untuk cek apakah recently active (dalam 24 jam terakhir)
    private boolean isRecentlyActive(String lastActiveStr) {
        if (lastActiveStr == null || lastActiveStr.isEmpty()) {
            return false;
        }

        try {
            Date lastActive = inputFormat.parse(lastActiveStr.replace("Z", ""));
            Date now = new Date();

            long diffInMillis = now.getTime() - lastActive.getTime();
            long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);

            return hours < 24; // Active dalam 24 jam terakhir

        } catch (ParseException e) {
            return false;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvName, tvEmail, tvJoinDate, tvTotalRecommendations, tvLastActive;

        ViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.card_doctor);
            tvName = view.findViewById(R.id.tv_doctor_name);
            tvEmail = view.findViewById(R.id.tv_doctor_email);
            tvJoinDate = view.findViewById(R.id.tv_join_date);
            tvTotalRecommendations = view.findViewById(R.id.tv_total_recommendations);
            tvLastActive = view.findViewById(R.id.tv_last_active);
        }
    }
}