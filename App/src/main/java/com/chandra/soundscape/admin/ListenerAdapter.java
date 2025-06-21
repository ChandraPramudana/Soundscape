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

public class ListenerAdapter extends RecyclerView.Adapter<ListenerAdapter.ViewHolder> {

    private List<ListenerListFragment.Listener> listenerList;
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));

    public ListenerAdapter(List<ListenerListFragment.Listener> listenerList) {
        this.listenerList = listenerList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listener, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ListenerListFragment.Listener listener = listenerList.get(position);

        // Set nama
        holder.tvName.setText(listener.getName());

        // Set email
        holder.tvEmail.setText(listener.getEmail());

        // Format dan set tanggal bergabung
        String joinDate = formatDate(listener.getJoinDate());
        holder.tvJoinDate.setText("Bergabung: " + joinDate);

        // Set total played (dalam menit)
        String totalPlayed = formatPlayTime(listener.getTotalPlayed());
        holder.tvTotalPlayed.setText("Total Dengar: " + totalPlayed);

        // Format dan set last active dengan status
        String lastActiveText = formatLastActive(listener.getLastActive());
        holder.tvLastActive.setText(lastActiveText);

        // Set warna berdasarkan aktivitas terakhir
        if (isRecentlyActive(listener.getLastActive())) {
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.success));
        } else {
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return listenerList != null ? listenerList.size() : 0;
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

    // Helper method untuk format waktu putar
    private String formatPlayTime(int totalMinutes) {
        if (totalMinutes == 0) {
            return "0 menit";
        }

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d jam %d menit", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%d menit", minutes);
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
        TextView tvName, tvEmail, tvJoinDate, tvTotalPlayed, tvLastActive;

        ViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.card_listener);
            tvName = view.findViewById(R.id.tv_listener_name);
            tvEmail = view.findViewById(R.id.tv_listener_email);
            tvJoinDate = view.findViewById(R.id.tv_join_date);
            tvTotalPlayed = view.findViewById(R.id.tv_total_played);
            tvLastActive = view.findViewById(R.id.tv_last_active);
        }
    }
}