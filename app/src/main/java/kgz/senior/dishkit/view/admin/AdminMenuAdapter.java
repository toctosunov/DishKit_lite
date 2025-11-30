package kgz.senior.dishkit.view.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Изменено с ImageButton на Button
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.MenuItem;

public class AdminMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Константы для типов элементов
    private static final int VIEW_TYPE_CATEGORY_HEADER = 0;
    private static final int VIEW_TYPE_MENU_ITEM = 1;

    private List<Object> displayList = new ArrayList<>(); // Список для отображения: может содержать String (категория) или MenuItem
    private OnMenuItemActionListener listener;

    public interface OnMenuItemActionListener {
        void onEditClick(MenuItem item);
        void onDeleteClick(MenuItem item);
    }

    public void setOnMenuItemActionListener(OnMenuItemActionListener listener) {
        this.listener = listener;
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        // Очищаем предыдущий список
        this.displayList.clear();

        // Группируем блюда по категориям
        Map<String, List<MenuItem>> groupedItems = new LinkedHashMap<>();
        for (MenuItem item : menuItems) {
            String category = item.getCategory() != null && !item.getCategory().isEmpty() ? item.getCategory() : "Без категории";
            if (!groupedItems.containsKey(category)) {
                groupedItems.put(category, new ArrayList<>());
            }
            Objects.requireNonNull(groupedItems.get(category)).add(item);
        }

        // Формируем displayList для RecyclerView: сначала заголовок, потом элементы
        for (Map.Entry<String, List<MenuItem>> entry : groupedItems.entrySet()) {
            displayList.add(entry.getKey()); // Добавляем заголовок категории (String)
            displayList.addAll(entry.getValue()); // Добавляем элементы блюд
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (displayList.get(position) instanceof String) {
            return VIEW_TYPE_CATEGORY_HEADER;
        } else {
            return VIEW_TYPE_MENU_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_CATEGORY_HEADER) {
            View view = inflater.inflate(R.layout.item_category_header, parent, false);
            return new CategoryHeaderViewHolder(view);
        } else {
            // ИЗМЕНЕНО: используем item_admin_menu_dish.xml
            View view = inflater.inflate(R.layout.item_admin_menu_dish, parent, false);
            return new MenuItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_CATEGORY_HEADER) {
            CategoryHeaderViewHolder categoryHolder = (CategoryHeaderViewHolder) holder;
            String categoryTitle = (String) displayList.get(position);
            categoryHolder.categoryTitle.setText(categoryTitle);
        } else {
            MenuItemViewHolder itemHolder = (MenuItemViewHolder) holder;
            MenuItem item = (MenuItem) displayList.get(position);

            // ИЗМЕНЕНО: Привязка данных к новым TextView из item_admin_menu_dish.xml
            itemHolder.adminDishNameTextView.setText(item.getName());
            itemHolder.adminDishPriceTextView.setText(String.format("Цена: %.2f сом", item.getPrice())); // Предполагаем "сом"
            itemHolder.adminDishVisibilityTextView.setText(item.isVisible() ? "Видимость: Да" : "Видимость: Нет");


            // Загрузка изображения
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(itemHolder.itemView.getContext())
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_logo) // Заглушка
                        .error(R.drawable.ic_logo)      // Изображение ошибки
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(itemHolder.adminDishImageView); // ИЗМЕНЕНО: на adminDishImageView
            } else {
                itemHolder.adminDishImageView.setImageResource(R.drawable.ic_logo); // ИЗМЕНЕНО: на adminDishImageView
            }

            // Обработчики кликов
            itemHolder.editDishButton.setOnClickListener(v -> { // ИЗМЕНЕНО: на editDishButton
                if (listener != null) {
                    listener.onEditClick(item);
                }
            });

            itemHolder.deleteDishButton.setOnClickListener(v -> { // ИЗМЕНЕНО: на deleteDishButton
                if (listener != null) {
                    listener.onDeleteClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // --- ViewHolders ---

    // ViewHolder для заголовка категории
    static class CategoryHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTitle;
        CategoryHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.categoryTitleTextView);
        }
    }

    // ViewHolder для элемента блюда (ИЗМЕНЕНО для item_admin_menu_dish.xml)
    static class MenuItemViewHolder extends RecyclerView.ViewHolder {
        ImageView adminDishImageView; // ИЗМЕНЕНО
        TextView adminDishNameTextView, adminDishPriceTextView, adminDishVisibilityTextView; // ИЗМЕНЕНО
        Button editDishButton, deleteDishButton; // ИЗМЕНЕНО с ImageButton на Button

        MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            // ИЗМЕНЕНО: Соответствие ID из item_admin_menu_dish.xml
            adminDishImageView = itemView.findViewById(R.id.adminDishImageView);
            adminDishNameTextView = itemView.findViewById(R.id.adminDishNameTextView);
            adminDishPriceTextView = itemView.findViewById(R.id.adminDishPriceTextView);
            adminDishVisibilityTextView = itemView.findViewById(R.id.adminDishVisibilityTextView);
            editDishButton = itemView.findViewById(R.id.editDishButton);
            deleteDishButton = itemView.findViewById(R.id.deleteDishButton);
        }
    }
}