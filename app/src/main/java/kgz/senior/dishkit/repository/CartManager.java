package kgz.senior.dishkit.repository;

import kgz.senior.dishkit.model.CartItem;
import kgz.senior.dishkit.model.MenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Синглтон для управления корзиной гостя.
 * Данные хранятся только в памяти приложения (нет сохранения в Firebase).
 */
public class CartManager {

    private static CartManager instance;
    private final List<CartItem> cartItems;

    private CartManager() {
        cartItems = new ArrayList<>();
    }

    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    /**
     * Добавляет или увеличивает количество блюда в корзине.
     * @param item Блюдо, которое нужно добавить.
     */
    public void addToCart(MenuItem item) {
        if (item == null || item.getId() == null) return;

        for (CartItem cartItem : cartItems) {
            // Проверяем, если блюдо уже в корзине по ID
            if (cartItem.getMenuItem().getId().equals(item.getId())) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                return;
            }
        }
        // Если блюда нет, добавляем новое
        cartItems.add(new CartItem(item, 1));
    }

    /**
     * Удаляет CartItem из корзины.
     * @param cartItem Элемент корзины, который нужно удалить.
     */
    public void removeCartItem(CartItem cartItem) {
        cartItems.remove(cartItem);
    }

    /**
     * Обновляет количество элемента в корзине.
     * Если newQuantity <= 0, элемент удаляется.
     */
    public void updateQuantity(CartItem cartItem, int newQuantity) {
        if (newQuantity <= 0) {
            removeCartItem(cartItem);
        } else {
            cartItem.setQuantity(newQuantity);
        }
    }

    public void clearCart() {
        cartItems.clear();
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    /**
     * Рассчитывает общую стоимость всех блюд в корзине.
     */
    public double getTotalCost() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }

    public boolean isEmpty() {
        return cartItems.isEmpty();
    }
}