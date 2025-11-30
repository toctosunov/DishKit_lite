// P:\local\AndroidStudio\DishKit\app\src\main\java\kgz\senior\dishkit\view\GuestCartAdapter.java
package kgz.senior.dishkit.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.CartItem;
import kgz.senior.dishkit.repository.CartManager;

public class GuestCartAdapter extends RecyclerView.Adapter<GuestCartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private final CartUpdateListener listener;

    /**
     * Интерфейс для оповещения DialogFragment об изменении корзины.
     */
    public interface CartUpdateListener {
        void onCartItemQuantityChanged();
        void onCartEmpty();
    }

    public GuestCartAdapter(List<CartItem> cartItems, CartUpdateListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    public void setCartItems(List<CartItem> newItems) {
        this.cartItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_dish, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        Context context = holder.itemView.getContext();

        holder.dishNameTextView.setText(item.getMenuItem().getName());

        // Цена за единицу
        String pricePerUnit = String.format(Locale.getDefault(), "%.2f сом / шт", item.getMenuItem().getPrice());
        holder.dishPriceTextView.setText(pricePerUnit);

        // Количество
        holder.quantityTextView.setText(String.valueOf(item.getQuantity()));

        // Общая стоимость для этого элемента
        String totalItemPrice = String.format(Locale.getDefault(), "Общая: %.2f сом", item.getTotalPrice());
        holder.itemTotalTextView.setText(totalItemPrice);

        // --- Обработчики кнопок ---

        // Кнопка УВЕЛИЧИТЬ (+)
        holder.buttonPlus.setOnClickListener(v -> {
            CartManager.getInstance().updateQuantity(item, item.getQuantity() + 1);
            // Обновляем только текущий элемент (более эффективно)
            notifyItemChanged(position);
            listener.onCartItemQuantityChanged(); // Сообщаем диалогу обновить итог
        });

        // Кнопка УМЕНЬШИТЬ (-)
        holder.buttonMinus.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() - 1;

            if (newQuantity > 0) {
                CartManager.getInstance().updateQuantity(item, newQuantity);
                notifyItemChanged(position); // Обновляем текущий элемент
                listener.onCartItemQuantityChanged(); // Сообщаем диалогу обновить итог
            } else {
                // Если количество становится 0, удаляем элемент
                CartManager.getInstance().removeCartItem(item);

                // Проверяем, опустела ли корзина, и оповещаем слушателя
                if (CartManager.getInstance().isEmpty()) {
                    listener.onCartEmpty();
                } else {
                    // Если корзина не пуста, но элемент удален
                    cartItems.remove(position);
                    notifyItemRemoved(position);
                    listener.onCartItemQuantityChanged();
                }

                Toast.makeText(context, item.getMenuItem().getName() + " удален из списка.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView dishNameTextView;
        TextView dishPriceTextView;
        TextView itemTotalTextView;
        TextView quantityTextView;
        MaterialButton buttonMinus;
        MaterialButton buttonPlus;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            dishNameTextView = itemView.findViewById(R.id.cartDishNameTextView);
            dishPriceTextView = itemView.findViewById(R.id.cartDishPriceTextView);
            itemTotalTextView = itemView.findViewById(R.id.cartItemTotalTextView);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            buttonMinus = itemView.findViewById(R.id.buttonMinus);
            buttonPlus = itemView.findViewById(R.id.buttonPlus);
        }
    }
}