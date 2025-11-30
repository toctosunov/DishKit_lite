// P:\local\AndroidStudio\DishKit\app\src\main\java\kgz\senior\dishkit\view\GuestCartDialogFragment.java
package kgz.senior.dishkit.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.repository.CartManager;

// Реализуем интерфейс из адаптера
public class GuestCartDialogFragment extends DialogFragment implements GuestCartAdapter.CartUpdateListener {

    public static final String TAG = "GuestCartDialogFragment";

    private CartManager cartManager;
    private GuestCartAdapter adapter;

    private RecyclerView cartRecyclerView;
    private TextView totalCostTextView;
    private TextView buttonClearCart;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Используем тему без заголовка и с закругленными углами
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_DishKit);
        cartManager = CartManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_guest_cart, container, false);

        cartRecyclerView = view.findViewById(R.id.cartRecyclerView);
        totalCostTextView = view.findViewById(R.id.totalTextView);
        buttonClearCart = view.findViewById(R.id.clearCartButton);
        MaterialButton buttonClose = view.findViewById(R.id.checkoutButton);

        // Инициализация RecyclerView
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GuestCartAdapter(cartManager.getCartItems(), this); // Передаем себя как слушателя
        cartRecyclerView.setAdapter(adapter);

        updateUIState();

        buttonClose.setOnClickListener(v -> dismiss());

        buttonClearCart.setOnClickListener(v -> showClearCartConfirmation());

        return view;
    }

    /**
     * Показывает диалог подтверждения очистки корзины.
     */
    private void showClearCartConfirmation() {
        if (cartManager.isEmpty()) {
            Toast.makeText(getContext(), "Список уже пуст.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Очистить список?")
                .setMessage("Вы уверены, что хотите удалить все блюда из списка? Это действие нельзя отменить.")
                .setPositiveButton("Очистить", (dialog, which) -> {
                    cartManager.clearCart();
                    Toast.makeText(getContext(), "Список блюд очищен.", Toast.LENGTH_SHORT).show();
                    // Поскольку корзина пуста, вызываем метод для закрытия
                    onCartEmpty();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    /**
     * Обновляет отображение списка (пуст/не пуст) и итоговой суммы.
     */
    private void updateUIState() {
        if (cartManager.isEmpty()) {
            cartRecyclerView.setVisibility(View.GONE);
            totalCostTextView.setText(String.format(Locale.getDefault(), "%.2f сом", 0.00));
            buttonClearCart.setEnabled(false);
        } else {
            // Обновляем адаптер, чтобы убедиться, что он показывает актуальные данные
            adapter.setCartItems(cartManager.getCartItems());

            cartRecyclerView.setVisibility(View.VISIBLE);
            buttonClearCart.setEnabled(true);
            updateTotalCost();
        }

        // Также сообщаем MainActivity, что корзина обновилась
        if (getActivity() instanceof UserMenuAdapter.CartActionListener) {
            ((UserMenuAdapter.CartActionListener) getActivity()).onCartUpdated();
        }
    }

    /**
     * Обновляет только текст итоговой стоимости.
     */
    private void updateTotalCost() {
        double total = cartManager.getTotalCost();
        String totalText = String.format(Locale.getDefault(), "%.2f сом", total);
        totalCostTextView.setText(totalText);
    }

    // --- Реализация интерфейса CartUpdateListener ---

    @Override
    public void onCartItemQuantityChanged() {
        // Вызывается из адаптера при изменении количества или удалении
        updateTotalCost();
    }

    @Override
    public void onCartEmpty() {
        // Вызывается из адаптера или при нажатии "Очистить"
        dismiss(); // Закрываем диалог, если корзина стала пустой

        // Также сообщаем MainActivity
        if (getActivity() instanceof UserMenuAdapter.CartActionListener) {
            ((UserMenuAdapter.CartActionListener) getActivity()).onCartUpdated();
        }
    }
}