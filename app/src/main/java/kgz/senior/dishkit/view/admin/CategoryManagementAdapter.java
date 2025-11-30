package kgz.senior.dishkit.view.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.Category;

public class CategoryManagementAdapter extends RecyclerView.Adapter<CategoryManagementAdapter.CategoryViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private OnCategoryActionListener listener;

    public CategoryManagementAdapter(List<Category> categories, OnCategoryActionListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public interface OnCategoryActionListener {
        void onEditClick(Category category);
        void onDeleteClick(Category category);
    }

    public void setOnCategoryActionListener(OnCategoryActionListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        this.categories = new ArrayList<>(categories); // Создаем копию
        // Сортируем по имени для удобства в UI диалога
        this.categories.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_management, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.categoryNameTextView.setText(category.getName());

        holder.editCategoryButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(category);
            }
        });

        holder.deleteCategoryButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameTextView;
        Button editCategoryButton, deleteCategoryButton;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameTextView = itemView.findViewById(R.id.categoryNameTextView);
            editCategoryButton = itemView.findViewById(R.id.editCategoryButton);
            deleteCategoryButton = itemView.findViewById(R.id.deleteCategoryButton);
        }
    }
}