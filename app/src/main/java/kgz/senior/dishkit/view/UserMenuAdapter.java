package kgz.senior.dishkit.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Добавили для обратной связи

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton; // Импорт кнопки
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.MenuItem;
import kgz.senior.dishkit.repository.CartManager; // Импорт менеджера корзины

public class UserMenuAdapter extends RecyclerView.Adapter<UserMenuAdapter.MenuViewHolder> {

    private List<MenuItem> menuItems;

    // Поля для хранения настроек видимости
    private boolean isImageVisible = true;
    private boolean isDescriptionVisible = true;
    private boolean isPriceVisible = true;
    private boolean isGuestCartVisible = true; // НОВОЕ ПОЛЕ для видимости корзины

    public UserMenuAdapter(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    public void setMenuItems(List<MenuItem> newItems) {
        this.menuItems = newItems;
        notifyDataSetChanged();
    }

    // Обновленный метод для установки настроек приложения
    public void setAppSettings(boolean isImageVisible, boolean isDescriptionVisible, boolean isPriceVisible, boolean isGuestCartVisible) {
        this.isImageVisible = isImageVisible;
        this.isDescriptionVisible = isDescriptionVisible;
        this.isPriceVisible = isPriceVisible;
        this.isGuestCartVisible = isGuestCartVisible; // Устанавливаем флаг корзины
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);

        holder.dishNameTextView.setText(item.getName());

        // 1. Управление видимостью ИЗОБРАЖЕНИЯ
        // ... (существующая логика) ...
        if (isImageVisible) {
            holder.dishImageView.setVisibility(View.VISIBLE);
            String imageUrl = item.getImageUrl();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_logo)
                        .error(R.drawable.ic_logo)
                        .into(holder.dishImageView);
            } else {
                holder.dishImageView.setImageResource(R.drawable.ic_logo);
            }
        } else {
            holder.dishImageView.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext()).clear(holder.dishImageView);
        }

        // 2. Управление видимостью ОПИСАНИЯ
        // ... (существующая логика) ...
        if (isDescriptionVisible && item.getDescription() != null && !item.getDescription().isEmpty()) {
            holder.dishDescriptionTextView.setText(item.getDescription());
            holder.dishDescriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.dishDescriptionTextView.setVisibility(View.GONE);
        }

        // 3. Управление видимостью ЦЕНЫ
        if (isPriceVisible) {
            String priceText = String.format("%.2f сом", item.getPrice());
            holder.dishPriceTextView.setText(priceText);
            holder.dishPriceTextView.setVisibility(View.VISIBLE);
        } else {
            holder.dishPriceTextView.setVisibility(View.GONE);
        }

        // --- 4. Управление видимостью КНОПКИ КОРЗИНЫ ---
        if (isGuestCartVisible) {
            holder.addToCartButton.setVisibility(View.VISIBLE);

            // Логика добавления блюда в корзину
            holder.addToCartButton.setOnClickListener(v -> {
                CartManager.getInstance().addToCart(item);
                Toast.makeText(holder.itemView.getContext(),
                        item.getName() + " добавлен в корзину!",
                        Toast.LENGTH_SHORT).show();
                // Можно добавить уведомление об обновлении корзины в MainActivity
                if (holder.itemView.getContext() instanceof CartActionListener) {
                    ((CartActionListener) holder.itemView.getContext()).onCartUpdated();
                }
            });

        } else {
            holder.addToCartButton.setVisibility(View.GONE);
            holder.addToCartButton.setOnClickListener(null); // Очищаем слушатель
        }
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public static class MenuViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView dishImageView;
        TextView dishNameTextView;
        TextView dishDescriptionTextView;
        TextView dishPriceTextView;
        MaterialButton addToCartButton; // Добавили кнопку

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            dishImageView = itemView.findViewById(R.id.dishImageView);
            dishNameTextView = itemView.findViewById(R.id.dishNameTextView);
            dishDescriptionTextView = itemView.findViewById(R.id.dishDescriptionTextView);
            dishPriceTextView = itemView.findViewById(R.id.dishPriceTextView);
            addToCartButton = itemView.findViewById(R.id.addToCartButton); // Находим кнопку
        }
    }

    /** * Интерфейс для оповещения Activity об изменении корзины.
     * MainActivity должна его реализовать.
     */
    public interface CartActionListener {
        void onCartUpdated();
    }
}