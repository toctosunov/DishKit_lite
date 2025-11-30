package kgz.senior.dishkit.utils;

public class Constants {
    // Коллекции Firestore
    public static final String COLLECTION_MENU_ITEMS = "menu_items";
    public static final String COLLECTION_CONFIG = "config";

    // Документы Firestore
    public static final String DOCUMENT_MAIN_SETTINGS = "main_settings";

    // Поля AppSettings
    public static final String FIELD_CAFE_NAME = "cafeName";
    public static final String FIELD_LOGO_URL = "logoUrl";
    public static final String FIELD_SHOW_PRICE = "showPrice";
    public static final String FIELD_SHOW_DESCRIPTION = "showDescription";
    public static final String FIELD_SHOW_IMAGE = "showImage";
    public static final String FIELD_COLOR_BACKGROUND_HEX = "colorBackgroundHex";
    public static final String FIELD_COLOR_PRIMARY_HEX = "colorPrimaryHex";
    public static final String FIELD_COLOR_TEXT_PRIMARY_HEX = "colorTextPrimaryHex";
    public static final String FIELD_COLOR_TEXT_SECONDARY_HEX = "colorTextSecondaryHex";
    public static final String FIELD_COLOR_PRICE_HEX = "colorPriceHex";

    // Поля MenuItem
    public static final String FIELD_MENU_ITEM_NAME = "name";
    public static final String FIELD_MENU_ITEM_DESCRIPTION = "description";
    public static final String FIELD_MENU_ITEM_PRICE = "price";
    public static final String FIELD_MENU_ITEM_IMAGE_URL = "imageUrl";
    public static final String FIELD_MENU_ITEM_IS_VISIBLE = "isVisible";
    public static final String FIELD_MENU_ITEM_ORDER = "order";

    // Пути Firebase Storage
    public static final String STORAGE_PATH_LOGOS = "logos/";
    public static final String STORAGE_PATH_DISH_IMAGES = "dish_images/";
}