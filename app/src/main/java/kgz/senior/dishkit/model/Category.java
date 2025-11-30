package kgz.senior.dishkit.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Objects;

public class Category implements Serializable {

    @Exclude // Исключаем поле 'id' из автоматического маппинга в Firestore
    private String id; // ID документа Firestore
    private String name;
    private int order;
    private String parentId; // <-- НОВОЕ ПОЛЕ: ID родительской категории (для подкатегорий)

    public Category() {
        // Пустой конструктор, необходимый для Firestore
    }

    public Category(String id, String name, int order, String parentId) { // <-- ОБНОВЛЕННЫЙ КОНСТРУКТОР
        this.id = id;
        this.name = name;
        this.order = order;
        this.parentId = parentId;
    }

    // Вспомогательный конструктор, если создаем без ID (для новых подкатегорий)
    public Category(String name, int order, String parentId) {
        this(null, name, order, parentId);
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    // НОВЫЕ ГЕТТЕР И СЕТТЕР ДЛЯ parentId
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    // --- Методы equals и hashCode (для сравнения объектов, если нужно) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return order == category.order &&
                Objects.equals(id, category.id) &&
                Objects.equals(name, category.name) &&
                Objects.equals(parentId, category.parentId); // Включаем parentId в сравнение
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, order, parentId); // Включаем parentId в хеш
    }
}