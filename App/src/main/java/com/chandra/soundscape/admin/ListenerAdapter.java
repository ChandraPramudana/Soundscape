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
import java.util.TimeZone;

public class ListenerAdapter extends RecyclerView.Adapter<ListenerAdapter.ListenerViewHolder> {

    private List<ListenerListFragment.Listener> listenerList;
    // FIXED: Multiple date formats to handle Supabase timestamps
    private SimpleDateFormat[] inputFormats = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault()), // With microseconds and timezone
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()),    // With milliseconds and timezone
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),         // Without milliseconds but with timezone
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())             // Basic format
    };
    private SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public ListenerAdapter(List<ListenerListFragment.Listener> listenerList) {
        this.listenerList = listenerList;
        // Set timezone for all formats
        for (SimpleDateFormat format : inputFormats) {
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        outputFormat.setTimeZone(TimeZone.getDefault());
    }

    @NonNull
    @Override
    public ListenerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listener, parent, false);
        return new ListenerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListenerViewHolder holder, int position) {
        ListenerListFragment.Listener listener = listenerList.get(position);
        holder.bind(listener);
    }

    @Override
    public int getItemCount() {
        return listenerList != null ? listenerList.size() : 0;
    }

    // Helper method to parse Supabase timestamp
    private Date parseSupabaseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        // Remove 'Z' suffix if present and replace with +00:00
        if (dateString.endsWith("Z")) {
            dateString = dateString.substring(0, dateString.length() - 1) + "+00:00";
        }

        // Try parsing with different formats
        for (SimpleDateFormat format : inputFormats) {
            try {
                return format.parse(dateString);
            } catch (ParseException e) {
                // Try next format
            }
        }

        // If all formats fail, try removing microseconds manually
        if (dateString.contains(".")) {
            String[] parts = dateString.split("\\.");
            if (parts.length > 1) {
                String beforeDot = parts[0];
                String afterDot = parts[1];

                // Extract timezone if present
                String timezone = "";
                if (afterDot.contains("+")) {
                    int plusIndex = afterDot.indexOf("+");
                    timezone = afterDot.substring(plusIndex);
                } else if (afterDot.contains("-") && afterDot.lastIndexOf("-") > 3) {
                    int minusIndex = afterDot.lastIndexOf("-");
                    timezone = afterDot.substring(minusIndex);
                }

                // Try parsing without microseconds
                try {
                    SimpleDateFormat fallbackFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    fallbackFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return fallbackFormat.parse(beforeDot);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    class ListenerViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvEmail, tvJoinDate, tvTotalPlayed, tvLastActive, tvAvatar;
        private MaterialCardView cardView;

        public ListenerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_listener);
            tvName = itemView.findViewById(R.id.tv_listener_name);
            tvEmail = itemView.findViewById(R.id.tv_listener_email);
            tvJoinDate = itemView.findViewById(R.id.tv_join_date);
            tvTotalPlayed = itemView.findViewById(R.id.tv_total_played);
            tvLastActive = itemView.findViewById(R.id.tv_last_active);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
        }

        public void bind(ListenerListFragment.Listener listener) {
            // Set name
            String displayName = listener.getName();
            if (displayName == null || displayName.isEmpty() || displayName.equals("Pengguna")) {
                // Use email prefix as fallback
                String email = listener.getEmail();
                if (email != null && email.contains("@")) {
                    displayName = email.substring(0, email.indexOf("@"));
                } else {
                    displayName = "Pengguna";
                }
            }
            tvName.setText(displayName);

            // Set avatar initial
            if (tvAvatar != null) {
                String initial = displayName.substring(0, 1).toUpperCase();
                tvAvatar.setText(initial);
            }

            // Set email
            tvEmail.setText(listener.getEmail());

            // Format and set join date
            Date joinDate = parseSupabaseDate(listener.getJoinDate());
            if (joinDate != null) {
                tvJoinDate.setText("Bergabung: " + outputFormat.format(joinDate));
            } else {
                tvJoinDate.setText("Bergabung: -");
            }

            // Set total played
            tvTotalPlayed.setText("Total diputar: " + listener.getTotalPlayed() + " lagu");

            // Set last active
            Date lastActiveDate = parseSupabaseDate(listener.getLastActive());
            if (lastActiveDate != null) {
                tvLastActive.setText("Terakhir aktif: " + outputFormat.format(lastActiveDate));
            } else {
                tvLastActive.setText("Terakhir aktif: -");
            }
        }
    }
}