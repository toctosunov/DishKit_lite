package kgz.senior.dishkit.model;

import com.google.firebase.firestore.PropertyName;

public class AppSettings {
    private String cafeName;
    private String logoUrl;

    // Настройки видимости
    private boolean isImageVisible = true;
    private boolean isDescriptionVisible = true;
    private boolean isPriceVisible = true;

    // НОВОЕ ПОЛЕ: для хранения выбранного типа сортировки блюд в админ-панели
    private String adminDishSortType;
    private boolean isGuestCartVisible;


    public AppSettings() {
        this.cafeName = "Меню";
        this.isImageVisible = true;
        this.isDescriptionVisible = true;
        this.isPriceVisible = true;
        this.adminDishSortType = "ALPHABETICAL"; // Дефолтное значение: сортировка по порядку
        this.isGuestCartVisible = true;

    }

    // --- Геттеры и сеттеры ---

    public String getCafeName() { return cafeName; }
    public void setCafeName(String cafeName) { this.cafeName = cafeName; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    // Явно указываем имя поля в Firestore, чтобы избежать дублирования
    @PropertyName("isImageVisible")
    public boolean isImageVisible() {
        return isImageVisible;
    }

    public void setImageVisible(boolean imageVisible) {
        isImageVisible = imageVisible;
    }

    // Явно указываем имя поля в Firestore
    @PropertyName("isDescriptionVisible")
    public boolean isDescriptionVisible() {
        return isDescriptionVisible;
    }

    public void setDescriptionVisible(boolean descriptionVisible) {
        isDescriptionVisible = descriptionVisible;
    }

    // Явно указываем имя поля в Firestore
    @PropertyName("isPriceVisible")
    public boolean isPriceVisible() {
        return isPriceVisible;
    }

    public void setPriceVisible(boolean priceVisible) {
        isPriceVisible = priceVisible;
    }
    @PropertyName("isGuestCartVisible")
    public boolean isGuestCartVisible() {
        return isGuestCartVisible;
    }

    public String getAdminDishSortType() {
        return adminDishSortType;
    }

    public void setAdminDishSortType(String adminDishSortType) {
        this.adminDishSortType = adminDishSortType;
    }

}

