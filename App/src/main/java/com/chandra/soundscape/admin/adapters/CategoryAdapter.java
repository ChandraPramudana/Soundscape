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
import com.chandra.soundscape.models.MusicCategory;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private Context context;
    private List<MusicCategory> categories;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(MusicCategory category);
    }

    public CategoryAdapter(Context context, List<MusicCategory> categories) {
        this.context = context;
        this.categories = categories;
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicCategory category = categories.get(position);
        holder.bind(category);

        // Set click listener
        holder.cardCategory.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public void updateData(List<MusicCategory> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardCategory;
        ImageView categoryIcon;
        TextView categoryName;
        TextView categoryDescription;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCategory = itemView.findViewById(R.id.card_category);
            categoryIcon = itemView.findViewById(R.id.iv_category_icon);
            categoryName = itemView.findViewById(R.id.tv_category_name);
            categoryDescription = itemView.findViewById(R.id.tv_category_description);
        }

        void bind(MusicCategory category) {
            // Set icon if available
            if (category.getIconResId() != 0) {
                categoryIcon.setImageResource(category.getIconResId());
            } else {
                categoryIcon.setImageResource(R.drawable.ic_category_default);
            }

            // Set name
            categoryName.setText(category.getName());

            // Set description if available
            if (categoryDescription != null && category.getDescription() != null
                    && !category.getDescription().isEmpty()) {
                categoryDescription.setText(category.getDescription());
                categoryDescription.setVisibility(View.VISIBLE);
            } else {
                if (categoryDescription != null) {
                    categoryDescription.setVisibility(View.GONE);
                }
            }
        }
    }
}