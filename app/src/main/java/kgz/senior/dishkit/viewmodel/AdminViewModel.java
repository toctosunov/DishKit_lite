package kgz.senior.dishkit.viewmodel;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import kgz.senior.dishkit.model.AppSettings;
import kgz.senior.dishkit.model.Category;
import kgz.senior.dishkit.model.MenuItem;

public class AdminViewModel extends ViewModel {

    private static final String TAG = "AdminViewModel";

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference appSettingsRef = db.collection("appSettings");
    private final CollectionReference menuItemsRef = db.collection("menuItems");
    private final CollectionReference categoriesRef = db.collection("categories");
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final StorageReference storageRef = storage.getReference();

    private final MutableLiveData<AppSettings> appSettings = new MutableLiveData<>();
    private final MutableLiveData<List<MenuItem>> menuItems = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>();

    public AdminViewModel() {
        isLoading.setValue(false);
        operationSuccess.setValue(false);
        loginSuccess.setValue(false);
        logoutSuccess.setValue(false);

        // Загрузка настроек приложения
        appSettingsRef.document("currentSettings").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed for AppSettings", e);
                error.setValue("Ошибка загрузки настроек: " + e.getLocalizedMessage());
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                AppSettings settings = snapshot.toObject(AppSettings.class);
                if (settings != null) { // Добавлена проверка на null
                    appSettings.setValue(settings);
                }
            } else {
                Log.d(TAG, "Current AppSettings data: null, creating default.");
                // Создаем дефолтные настройки, если документа нет
                AppSettings defaultSettings = new AppSettings();
                appSettings.setValue(defaultSettings);
                // Опционально: можно сохранить эти дефолтные настройки в базу, если их нет
                // saveAppSettings(defaultSettings); // Вызовет запись в Firestore
            }
        });

        // Загрузка блюд (отсортированных по порядку, адаптер потом сам их сгруппирует по категориям)
        menuItemsRef.orderBy("order", Query.Direction.ASCENDING).addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed for MenuItems", e);
                error.setValue("Ошибка загрузки меню: " + e.getLocalizedMessage());
                return;
            }
            if (snapshots != null) {
                List<MenuItem> items = new ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                    MenuItem item = doc.toObject(MenuItem.class);
                    if (item != null) {
                        item.setId(doc.getId()); // Устанавливаем ID документа Firestore
                        items.add(item);
                    }
                }
                menuItems.setValue(items);
            }
        });

        // Загрузка категорий (отсортированных по имени)
        categoriesRef.orderBy("name", Query.Direction.ASCENDING).addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed for Categories", e);
                error.setValue("Ошибка загрузки категорий: " + e.getLocalizedMessage());
                return;
            }
            if (snapshots != null) {
                List<Category> categoryList = new ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                    Category category = doc.toObject(Category.class);
                    if (category != null) {
                        category.setId(doc.getId());
                        categoryList.add(category);
                    }
                }

                // Сортировка: сначала основные категории, потом подкатегории
                categoryList.sort((c1, c2) -> {
                    boolean isC1Parent = c1.getParentId() == null || c1.getParentId().isEmpty();
                    boolean isC2Parent = c2.getParentId() == null || c2.getParentId().isEmpty();

                    if (isC1Parent && !isC2Parent) return -1;
                    if (!isC1Parent && isC2Parent) return 1;

                    return c1.getName().compareToIgnoreCase(c2.getName());
                });

                categories.setValue(categoryList);
            }
        });
    }

    // --- Геттеры LiveData ---
    public LiveData<AppSettings> getAppSettings() {
        return appSettings;
    }

    public LiveData<List<MenuItem>> getMenuItems() {
        return menuItems;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<Boolean> getLogoutSuccess() {
        return logoutSuccess;
    }

    // --- Методы для Firebase Auth ---
    public void login(String email, String password) {
        isLoading.setValue(true);
        error.setValue(null);
        loginSuccess.setValue(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        loginSuccess.setValue(true);
                        loginSuccess.setValue(null);
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        error.setValue("Ошибка входа: " + Objects.requireNonNull(task.getException()).getLocalizedMessage());
                        error.setValue(null);
                    }
                });
    }

    public void logout() {
        firebaseAuth.signOut();
        logoutSuccess.setValue(true);
        logoutSuccess.setValue(null);
    }

    // --- Методы для AppSettings ---
    public void saveAppSettings(AppSettings settings) {
        isLoading.setValue(true);
        appSettingsRef.document("currentSettings").set(settings)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Настройки успешно сохранены.");
                    operationSuccess.setValue(true);
                    operationSuccess.setValue(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при сохранении настроек: " + e.getMessage());
                    error.setValue("Ошибка при сохранении настроек: " + e.getLocalizedMessage());
                    error.setValue(null);
                })
                .addOnCompleteListener(task -> isLoading.setValue(false));
    }

    // НОВЫЙ МЕТОД: Обновление типа сортировки блюд в админ-панели
    public void updateAdminDishSortType(String sortType) {
        AppSettings currentSettings = appSettings.getValue();
        if (currentSettings == null) {
            currentSettings = new AppSettings();
        }
        if (!Objects.equals(currentSettings.getAdminDishSortType(), sortType)) {
            currentSettings.setAdminDishSortType(sortType);
            saveAppSettings(currentSettings);
        }
    }


    public void uploadLogo(Uri logoUri) {
        isLoading.setValue(true);
        String fileName = "logo_" + UUID.randomUUID().toString();
        StorageReference logoImageRef = storageRef.child("logos/" + fileName);

        logoImageRef.putFile(logoUri)
                .addOnSuccessListener(taskSnapshot -> logoImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    AppSettings currentSettings = appSettings.getValue();
                    if (currentSettings == null) {
                        currentSettings = new AppSettings();
                    }
                    currentSettings.setLogoUrl(uri.toString());
                    saveAppSettings(currentSettings);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка загрузки логотипа: " + e.getMessage());
                    error.setValue("Ошибка загрузки логотипа: " + e.getLocalizedMessage());
                    isLoading.setValue(false);
                    operationSuccess.setValue(null);
                    error.setValue(null);
                });
    }

    // --- Методы для MenuItems ---
    public void addOrUpdateMenuItem(MenuItem menuItem, @Nullable Uri imageUri) {
        isLoading.setValue(true);
        operationSuccess.setValue(false);

        if (imageUri != null) {
            String fileName = "dish_" + UUID.randomUUID().toString();
            StorageReference dishImageRef = storageRef.child("menu_items/" + fileName);

            dishImageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> dishImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        menuItem.setImageUrl(uri.toString());
                        saveMenuItemToFirestore(menuItem);
                    }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка загрузки изображения блюда: " + e.getMessage());
                        error.setValue("Ошибка загрузки изображения блюда: " + e.getLocalizedMessage());
                        isLoading.setValue(false);
                        operationSuccess.setValue(null);
                        error.setValue(null);
                    });
        } else {
            saveMenuItemToFirestore(menuItem);
        }
    }

    private void saveMenuItemToFirestore(MenuItem menuItem) {
        if (menuItem.getId() == null || menuItem.getId().isEmpty()) {
            // Если ID не установлен, генерируем новый
            String newId = db.collection("menuItems").document().getId();
            menuItem.setId(newId);
        }

        // Перед сохранением, проверяем наличие categoryId.
        // Если item.getCategoryId() == null, пытаемся найти ID категории по имени.
        // Это важно, если новая категория была только что создана, и ее ID еще не прокинулся.
        if (menuItem.getCategoryId() == null || menuItem.getCategoryId().isEmpty()) {
            categoriesRef.whereEqualTo("name", menuItem.getCategory())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            menuItem.setCategoryId(queryDocumentSnapshots.getDocuments().get(0).getId());
                        }
                        // Сохраняем блюдо после попытки найти categoryId
                        saveMenuItemWithResolvedCategoryId(menuItem);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка при поиске categoryId по имени: " + e.getMessage());
                        error.setValue("Ошибка: " + e.getLocalizedMessage());
                        // Сохраняем без categoryId, если поиск не удался
                        saveMenuItemWithResolvedCategoryId(menuItem);
                    });
        } else {
            saveMenuItemWithResolvedCategoryId(menuItem);
        }
    }

    private void saveMenuItemWithResolvedCategoryId(MenuItem menuItem) {
        menuItemsRef.document(menuItem.getId()).set(menuItem)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Блюдо успешно сохранено: " + menuItem.getName());
                    operationSuccess.setValue(true);
                    operationSuccess.setValue(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при сохранении блюда: " + e.getMessage());
                    error.setValue("Ошибка при сохранении блюда: " + e.getLocalizedMessage());
                    error.setValue(null);
                })
                .addOnCompleteListener(task -> isLoading.setValue(false));
    }


    public void deleteMenuItem(String itemId) {
        isLoading.setValue(true);
        operationSuccess.setValue(false);

        menuItemsRef.document(itemId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Блюдо успешно удалено: " + itemId);
                    operationSuccess.setValue(true);
                    operationSuccess.setValue(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при удалении блюда: " + e.getMessage());
                    error.setValue("Ошибка при удалении блюда: " + e.getLocalizedMessage());
                    error.setValue(null);
                })
                .addOnCompleteListener(task -> isLoading.setValue(false));
    }


    // --- НОВЫЕ МЕТОДЫ: Для управления категориями ---

    public void addOrUpdateCategory(Category category) {
        isLoading.setValue(true);
        operationSuccess.setValue(false);

        Query query = categoriesRef.whereEqualTo("name", category.getName().trim());
        if (category.getParentId() != null && !category.getParentId().isEmpty()) {
            query = query.whereEqualTo("parentId", category.getParentId());
        } else {
            query = query.whereEqualTo("parentId", null);
        }


        query.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            boolean isExistingCategoryBeingEdited = false;
                            for (com.google.firebase.firestore.DocumentSnapshot document : task.getResult().getDocuments()) {
                                if (category.getId() != null && category.getId().equals(document.getId())) {
                                    isExistingCategoryBeingEdited = true;
                                    break;
                                }
                            }

                            if (!isExistingCategoryBeingEdited) {
                                error.setValue("Категория с таким названием уже существует (в этой же родительской категории).");
                                error.setValue(null);
                                isLoading.setValue(false);
                                return;
                            }
                        }
                        saveCategoryToFirestore(category);

                    } else {
                        Log.e(TAG, "Ошибка проверки дубликатов категорий: " + Objects.requireNonNull(task.getException()).getMessage());
                        error.setValue("Ошибка проверки дубликатов категорий: " + Objects.requireNonNull(task.getException()).getLocalizedMessage());
                        error.setValue(null);
                        isLoading.setValue(false);
                    }
                });
    }

    private void saveCategoryToFirestore(Category category) {
        if (category.getId() == null || category.getId().isEmpty()) {
            category.setId(categoriesRef.document().getId());
        }

        categoriesRef.document(category.getId()).set(category)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Категория успешно сохранена: " + category.getName());
                    operationSuccess.setValue(true);
                    operationSuccess.setValue(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при сохранении категории: " + e.getMessage());
                    error.setValue("Ошибка при сохранении категории: " + e.getLocalizedMessage());
                    error.setValue(null);
                })
                .addOnCompleteListener(task -> isLoading.setValue(false));
    }

    public void deleteCategory(String categoryId) {
        isLoading.setValue(true);
        operationSuccess.setValue(false);

        categoriesRef.whereEqualTo("parentId", categoryId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<String> subCategoryIds = queryDocumentSnapshots.getDocuments().stream()
                                .map(com.google.firebase.firestore.DocumentSnapshot::getId)
                                .collect(Collectors.toList());

                        FirebaseFirestore.getInstance().runBatch(batch -> {
                                    for (String subId : subCategoryIds) {
                                        batch.delete(categoriesRef.document(subId));
                                    }
                                    batch.delete(categoriesRef.document(categoryId));
                                })
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Категория и ее подкатегории успешно удалены: " + categoryId);
                                    operationSuccess.setValue(true);
                                    operationSuccess.setValue(null);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Ошибка при пакетном удалении категорий: " + e.getMessage());
                                    error.setValue("Ошибка при удалении категории и подкатегорий: " + e.getLocalizedMessage());
                                    error.setValue(null);
                                })
                                .addOnCompleteListener(task -> isLoading.setValue(false));

                    } else {
                        categoriesRef.document(categoryId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Категория успешно удалена: " + categoryId);
                                    operationSuccess.setValue(true);
                                    operationSuccess.setValue(null);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Ошибка при удалении категории: " + e.getMessage());
                                    error.setValue("Ошибка при удалении категории: " + e.getLocalizedMessage());
                                    error.setValue(null);
                                })
                                .addOnCompleteListener(task -> isLoading.setValue(false));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при проверке подкатегорий перед удалением: " + e.getMessage());
                    error.setValue("Ошибка при удалении категории: " + e.getLocalizedMessage());
                    error.setValue(null);
                    isLoading.setValue(false);
                });
    }
}