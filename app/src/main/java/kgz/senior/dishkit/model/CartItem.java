package kgz.senior.dishkit.model;

import java.io.Serializable;

public class CartItem implements Serializable {
    private MenuItem menuItem;
    private int quantity;

    public CartItem(MenuItem menuItem, int quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    // --- Геттеры и Сеттеры ---

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Рассчитывает общую стоимость этого элемента (Цена * Количество).
     * @return общая стоимость.
     */
    public double getTotalPrice() {
        return menuItem.getPrice() * quantity;
    }
}