package kgz.senior.dishkit.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Objects;

public class MenuItem implements Serializable {

    @Exclude
    private String id;
    private String name;
    private String description;
    private String category;
    private String categoryId;
    private double price;
    private String imageUrl;
    private int order;
    private boolean isVisible = true;

    public MenuItem() {
        // Пустой конструктор, необходимый для Firestore
    }

    // Обновленный конструктор, если вы его используете
    public MenuItem(String id, String name, String description, String category, String categoryId,
                    double price, String imageUrl, int order, boolean isVisible) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.categoryId = categoryId; // Инициализируем новое поле
        this.price = price;
        this.imageUrl = imageUrl;
        this.order = order;
        this.isVisible = isVisible;
    }

    // Вспомогательный конструктор, если вы создаете объекты без ID
    public MenuItem(String name, String description, String category, String categoryId,
                    double price, String imageUrl, int order, boolean isVisible) {
        this(null, name, description, category, categoryId, price, imageUrl, order, isVisible);
    }

    // --- Геттеры и Сеттеры ---
    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // НОВЫЕ ГЕТТЕР И СЕТТЕР для categoryId
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    // --- Методы equals и hashCode (обновлены для включения categoryId) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return Double.compare(menuItem.price, price) == 0 &&
                order == menuItem.order &&
                isVisible == menuItem.isVisible &&
                Objects.equals(id, menuItem.id) &&
                Objects.equals(name, menuItem.name) &&
                Objects.equals(description, menuItem.description) &&
                Objects.equals(category, menuItem.category) &&
                Objects.equals(categoryId, menuItem.categoryId) && // Включаем categoryId в сравнение
                Objects.equals(imageUrl, menuItem.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, category, categoryId, price, imageUrl, order, isVisible); // Включаем categoryId в хеш
    }
}