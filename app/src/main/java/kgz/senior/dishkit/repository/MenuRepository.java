package kgz.senior.dishkit.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import kgz.senior.dishkit.model.AppSettings;
import kgz.senior.dishkit.model.MenuItem;
import kgz.senior.dishkit.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class MenuRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private MutableLiveData<List<MenuItem>> menuItemsLiveData = new MutableLiveData<>();
    private MutableLiveData<AppSettings> appSettingsLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private ListenerRegistration appSettingsListener;
    private ListenerRegistration menuItemsListener;

    public MenuRepository() {
        // Инициализируем слушателей при создании репозитория
        listenForAppSettings();
        listenForMenuItems();
    }

    // Слушатель для автоматического обновления настроек приложения
    private void listenForAppSettings() {
        if (appSettingsListener != null) {
            appSettingsListener.remove(); // Удаляем предыдущего слушателя, если есть
        }
        appSettingsListener = db.collection(Constants.COLLECTION_CONFIG)
                .document(Constants.DOCUMENT_MAIN_SETTINGS)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        errorLiveData.postValue("Ошибка загрузки настроек: " + e.getMessage());
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        AppSettings settings = documentSnapshot.toObject(AppSettings.class);
                        appSettingsLiveData.postValue(settings);
                    } else {
                        // Если документа нет, создадим дефолтные настройки
                        AppSettings defaultSettings = new AppSettings();
                        appSettingsLiveData.postValue(defaultSettings);
                        // Опционально: можно сохранить дефолтные настройки в базу, если их нет
                        // saveAppSettings(defaultSettings, null);
                    }
                });
    }

    // Слушатель для автоматического обновления списка блюд
    private void listenForMenuItems() {
        if (menuItemsListener != null) {
            menuItemsListener.remove();
        }
        loadingLiveData.postValue(true);
        menuItemsListener = db.collection(Constants.COLLECTION_MENU_ITEMS)
                .whereEqualTo(Constants.FIELD_MENU_ITEM_IS_VISIBLE, true) // Отображаем только видимые блюда
                .orderBy(Constants.FIELD_MENU_ITEM_ORDER, Query.Direction.ASCENDING) // Сортируем по полю order
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        errorLiveData.postValue("Ошибка загрузки меню: " + e.getMessage());
                        loadingLiveData.postValue(false);
                        return;
                    }
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<MenuItem> items = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            MenuItem item = doc.toObject(MenuItem.class);
                            if (item != null) {
                                item.setId(doc.getId()); // Сохраняем ID документа
                                items.add(item);
                            }
                        }
                        menuItemsLiveData.postValue(items);
                    } else {
                        menuItemsLiveData.postValue(new ArrayList<>()); // Пустой список, если нет блюд
                    }
                    loadingLiveData.postValue(false);
                });
    }

    // Методы для админки (будут использованы в AdminViewModel)

    /**
     * Сохраняет настройки приложения в Firestore.
     * @param settings Объект AppSettings для сохранения.
     * @param callback Callback для уведомления о завершении.
     */
    public void saveAppSettings(AppSettings settings, Callback<Void> callback) {
        db.collection(Constants.COLLECTION_CONFIG)
                .document(Constants.DOCUMENT_MAIN_SETTINGS)
                .set(settings)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Загружает изображение в Firebase Storage.
     * @param imageUri URI локального файла изображения.
     * @param path Путь в Storage (например, Constants.STORAGE_PATH_LOGOS).
     * @param fileName Имя файла в Storage.
     * @param callback Callback для получения URL загруженного изображения.
     */
    public void uploadImage(android.net.Uri imageUri, String path, String fileName, Callback<String> callback) {
        StorageReference ref = storage.getReference().child(path + fileName);
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    if (callback != null) callback.onSuccess(uri.toString());
                }))
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Ошибка загрузки изображения: " + e.getMessage());
                })
                .addOnProgressListener(snapshot -> {
                    // Можно показывать прогресс загрузки в UI
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    // Log.d("Upload", "Progress: " + progress + "%");
                });
    }

    /**
     * Добавляет или обновляет блюдо в Firestore.
     * @param item Объект MenuItem. Если id пустой, то добавляется, иначе обновляется.
     * @param callback Callback для уведомления о завершении.
     */
    public void addOrUpdateMenuItem(MenuItem item, Callback<Void> callback) {
        if (item.getId() == null || item.getId().isEmpty()) {
            // Добавление нового блюда
            db.collection(Constants.COLLECTION_MENU_ITEMS)
                    .add(item)
                    .addOnSuccessListener(documentReference -> {
                        if (callback != null) callback.onSuccess(null);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onFailure(e.getMessage());
                    });
        } else {
            // Обновление существующего блюда
            db.collection(Constants.COLLECTION_MENU_ITEMS)
                    .document(item.getId())
                    .set(item)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess(null);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onFailure(e.getMessage());
                    });
        }
    }

    /**
     * Удаляет блюдо из Firestore.
     * @param itemId ID блюда.
     * @param callback Callback для уведомления о завершении.
     */
    public void deleteMenuItem(String itemId, Callback<Void> callback) {
        db.collection(Constants.COLLECTION_MENU_ITEMS)
                .document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    // LiveData геттеры для UI
    public LiveData<List<MenuItem>> getMenuItemsLiveData() {
        return menuItemsLiveData;
    }

    public LiveData<AppSettings> getAppSettingsLiveData() {
        return appSettingsLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    // Интерфейс для коллбэков (для удобства работы с асинхронными операциями)
    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }

    // Очистка слушателей, когда репозиторий больше не нужен
    public void cleanup() {
        if (appSettingsListener != null) {
            appSettingsListener.remove();
        }
        if (menuItemsListener != null) {
            menuItemsListener.remove();
        }
    }
}