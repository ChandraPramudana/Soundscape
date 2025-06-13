package com.chandra.soundscape.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chandra.soundscape.R;
import com.chandra.soundscape.models.Journal;
import com.chandra.soundscape.models.JournalEvidence;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AdminJournalEvidenceAdapter extends RecyclerView.Adapter<AdminJournalEvidenceAdapter.EvidenceViewHolder> {
    private Context context;
    private List<JournalEvidence> evidenceList;
    private List<Journal> availableJournals;
    private OnEvidenceActionListener listener;

    public interface OnEvidenceActionListener {
        void onEditEvidence(int position);
        void onDeleteEvidence(int position);
    }

    public AdminJournalEvidenceAdapter(Context context, List<JournalEvidence> evidenceList, List<Journal> availableJournals) {
        this.context = context;
        this.evidenceList = evidenceList;
        this.availableJournals = availableJournals;
    }

    public void setOnEvidenceActionListener(OnEvidenceActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EvidenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_journal_evidence, parent, false);
        return new EvidenceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EvidenceViewHolder holder, int position) {
        JournalEvidence evidence = evidenceList.get(position);
        holder.bind(evidence, position);
    }

    @Override
    public int getItemCount() {
        return evidenceList.size();
    }

    class EvidenceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvJournalTitle, tvAuthors, tvEvidenceStrength, tvRelevanceDescription;
        private MaterialButton btnEdit, btnDelete;

        public EvidenceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvJournalTitle = itemView.findViewById(R.id.tv_journal_title);
            tvAuthors = itemView.findViewById(R.id.tv_authors);
            tvEvidenceStrength = itemView.findViewById(R.id.tv_evidence_strength);
            tvRelevanceDescription = itemView.findViewById(R.id.tv_relevance_description);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(JournalEvidence evidence, int position) {
            if (evidence.getJournal() != null) {
                tvJournalTitle.setText(evidence.getJournal().getTitle());
                tvAuthors.setText("By: " + evidence.getJournal().getAuthors());
            } else {
                tvJournalTitle.setText("Unknown Journal");
                tvAuthors.setText("Unknown Authors");
            }

            tvEvidenceStrength.setText("Strength: " +
                    evidence.getEvidenceStrength().toUpperCase());
            tvRelevanceDescription.setText(evidence.getRelevanceDescription());

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditEvidence(position);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteEvidence(position);
            });
        }
    }
}