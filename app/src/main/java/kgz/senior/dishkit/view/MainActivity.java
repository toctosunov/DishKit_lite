package kgz.senior.dishkit.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // ⬅️ Добавлен импорт Log
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Comparator; // ⬅️ Добавлен импорт Comparator
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.Category;
import kgz.senior.dishkit.model.MenuItem;
import kgz.senior.dishkit.repository.CartManager;
import kgz.senior.dishkit.view.admin.LoginActivity;
import kgz.senior.dishkit.viewmodel.AdminViewModel;

public class MainActivity extends AppCompatActivity implements CategoryListAdapter.OnCategorySelectedListener, UserMenuAdapter.CartActionListener {

    private AdminViewModel adminViewModel;
    private Toolbar mainToolbar;
    private ImageView toolbarLogo;
    private TextView toolbarTitle;
    private ProgressBar mainProgressBar;

    private RecyclerView categoriesRecyclerView;
    private CategoryListAdapter categoryListAdapter;
    private RecyclerView dishesRecyclerView;
    private UserMenuAdapter userMenuAdapter;
    private TextView emptyDishesTextView;

    private ExtendedFloatingActionButton cartButton;

    private List<Category> allCategories = new ArrayList<>();
    private List<MenuItem> allMenuItems = new ArrayList<>();
    private Category selectedCategory = null;

    // --- Переменные и Enum для сортировки ---
    private static final String TAG = "MainActivity"; // ⬅️ Добавлено
    private SortType currentSortType = SortType.ORDER; // ⬅️ Добавлено. Дефолтное значение

    private enum SortType { // ⬅️ Добавлено
        ORDER,
        ALPHABETICAL
    }
    // ----------------------------------------

    // --- Переменные для скрытой админки ---
    private int logoClickCount = 0;
    private static final int REQUIRED_CLICKS = 5;
    private static final long CLICK_TIMEOUT_MS = 1500;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable resetClickCountRunnable = () -> logoClickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainToolbar = findViewById(R.id.mainToolbar);
        toolbarLogo = findViewById(R.id.toolbarLogo);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        mainProgressBar = findViewById(R.id.mainProgressBar);
        cartButton = findViewById(R.id.cartButton);

        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        dishesRecyclerView = findViewById(R.id.dishesRecyclerView);
        emptyDishesTextView = findViewById(R.id.emptyDishesTextView);

        // Инициализация UserMenuAdapter
        dishesRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        userMenuAdapter = new UserMenuAdapter(new ArrayList<>());
        dishesRecyclerView.setAdapter(userMenuAdapter);

        // Инициализация CategoryListAdapter
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryListAdapter = new CategoryListAdapter(new ArrayList<>(), this);
        categoriesRecyclerView.setAdapter(categoryListAdapter);

        setSupportActionBar(mainToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // --- Подписка на LiveData из ViewModel ---

        adminViewModel.getAppSettings().observe(this, appSettings -> {
            if (appSettings != null) {
                // 1. Применяем видимость элементов меню, включая isGuestCartVisible
                if (userMenuAdapter != null) {
                    userMenuAdapter.setAppSettings(
                            appSettings.isImageVisible(),
                            appSettings.isDescriptionVisible(),
                            appSettings.isPriceVisible(),
                            appSettings.isGuestCartVisible()
                    );
                }

                // ⬅️ НОВОЕ: Чтение и установка типа сортировки из AppSettings
                String sortTypeString = appSettings.getAdminDishSortType();
                try {
                    SortType newSortType = SortType.valueOf(sortTypeString);
                    if (currentSortType != newSortType) {
                        currentSortType = newSortType;
                        Log.d(TAG, "Sort type updated from AppSettings: " + currentSortType);
                        updateDishesList(); // Обновляем список, если тип сортировки изменился
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Неизвестный тип сортировки в AppSettings: " + sortTypeString + ". Устанавливаю ORDER.", e);
                    currentSortType = SortType.ORDER; // Если ошибка, ставим по умолчанию
                }

                // 2. Управление видимостью кнопки корзины
                if (appSettings.isGuestCartVisible()) {
                    cartButton.setVisibility(View.VISIBLE);
                    updateCartIconState(); // Обновляем состояние кнопки корзины
                } else {
                    cartButton.setVisibility(View.GONE);
                }

                if (appSettings.getLogoUrl() != null && !appSettings.getLogoUrl().isEmpty()) {
                    Glide.with(this).load(appSettings.getLogoUrl()).into(toolbarLogo);
                    toolbarLogo.setVisibility(View.VISIBLE);
                } else {
                    toolbarLogo.setImageResource(R.drawable.ic_logo);
                    toolbarLogo.setVisibility(View.VISIBLE);
                }
                toolbarTitle.setText(appSettings.getCafeName());
            } else {
                toolbarTitle.setText("DishKit");
                toolbarLogo.setImageResource(R.drawable.ic_logo);
                toolbarLogo.setVisibility(View.VISIBLE);
                cartButton.setVisibility(View.GONE);
            }
        });

        adminViewModel.getCategories().observe(this, categoriesList -> {
            // Фильтруем список для CategoryListAdapter (убираем "Без категории" из начала)
            List<Category> filteredCategories = categoriesList.stream()
                    .filter(category -> !category.getName().equals("Без категории"))
                    .collect(Collectors.toCollection(ArrayList::new));

            // Добавляем "Без категории" в конец, если она была
            categoriesList.stream()
                    .filter(category -> category.getName().equals("Без категории"))
                    .findFirst()
                    .ifPresent(filteredCategories::add);

            allCategories = filteredCategories;
            categoryListAdapter.setCategories(allCategories);
        });

        adminViewModel.getMenuItems().observe(this, menuItems -> {
            allMenuItems = menuItems; // Сохраняем все блюда
            updateDishesList(); // Обновляем список блюд для текущей выбранной категории
        });


        adminViewModel.isLoading().observe(this, isLoading -> {
            mainProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        adminViewModel.getError().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, "Ошибка: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        cartButton.setOnClickListener(v -> {
            if (CartManager.getInstance().isEmpty()) {
                Toast.makeText(this, "Список пуст. Добавьте блюда, чтобы показать официанту.", Toast.LENGTH_SHORT).show();
            } else {
                // ОТКРЫТИЕ ДИАЛОГА КОРЗИНЫ
                GuestCartDialogFragment dialog = new GuestCartDialogFragment();
                dialog.show(getSupportFragmentManager(), GuestCartDialogFragment.TAG);
            }
        });

        // ...
        toolbarLogo.setOnClickListener(v -> {
            logoClickCount++;
            handler.removeCallbacks(resetClickCountRunnable);

            if (logoClickCount == REQUIRED_CLICKS) {
                Toast.makeText(this, "Открытие админ панели...", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                logoClickCount = 0;
            } else {
                handler.postDelayed(resetClickCountRunnable, CLICK_TIMEOUT_MS);
            }
        });

        updateCartIconState(); // Инициализация состояния иконки при запуске
    }

    // --- Реализация интерфейса OnCategorySelectedListener ---
    @Override
    public void onCategorySelected(Category category) {
        selectedCategory = category;
        updateDishesList(); // Обновляем правый список при выборе категории
    }

    // --- Метод для фильтрации и обновления списка блюд (ИЗМЕНЕН) ---
    private void updateDishesList() {
        if (selectedCategory == null || allMenuItems.isEmpty() || userMenuAdapter == null) {
            if (userMenuAdapter != null) {
                userMenuAdapter.setMenuItems(new ArrayList<>());
            }
            emptyDishesTextView.setVisibility(View.VISIBLE);
            emptyDishesTextView.setText(selectedCategory == null ?
                    "Выберите категорию" : "В категории \"" + (selectedCategory != null ? selectedCategory.getName() : "") + "\" пока нет блюд.");
            return;
        }

        final String selectedCategoryName = selectedCategory.getName();
        final String selectedCategoryId = selectedCategory.getId();

        // 1. Определяем список ИМЕН категорий, по которым нужно фильтровать.
        List<String> categoryNamesToFilterBy = new ArrayList<>();
        categoryNamesToFilterBy.add(selectedCategoryName.toLowerCase());

        // 1b. Проверяем, является ли выбранная категория основной (родительской)
        if (selectedCategory.getParentId() == null || selectedCategory.getParentId().isEmpty()) {
            allCategories.stream()
                    .filter(c -> Objects.equals(c.getParentId(), selectedCategoryId))
                    .map(c -> c.getName().toLowerCase())
                    .forEach(categoryNamesToFilterBy::add);
        }

        // 2. Фильтруем блюда
        List<MenuItem> filteredItems = allMenuItems.stream()
                .filter(item -> item.isVisible() && categoryNamesToFilterBy.contains(item.getCategory().toLowerCase()))
                // ⬅️ УДАЛЕНА СТАРАЯ ЖЕСТКАЯ СОРТИРОВКА
                .collect(Collectors.toList());

        // 3. ⬅️ ПРИМЕНЯЕМ ДИНАМИЧЕСКУЮ СОРТИРОВКУ
        List<MenuItem> sortedAndFilteredItems = applySortToFilteredItems(filteredItems);

        userMenuAdapter.setMenuItems(sortedAndFilteredItems);

        if (sortedAndFilteredItems.isEmpty()) {
            emptyDishesTextView.setVisibility(View.VISIBLE);
            emptyDishesTextView.setText("В категории \"" + selectedCategoryName + "\" и ее подразделах пока нет блюд.");

        } else {
            emptyDishesTextView.setVisibility(View.GONE);
        }
    }

    // --- НОВЫЙ МЕТОД СОРТИРОВКИ (Копия из DishesFragment) ---
    private List<MenuItem> applySortToFilteredItems(List<MenuItem> filteredItems) {
        if (filteredItems == null || filteredItems.isEmpty()) {
            return new ArrayList<>();
        }

        List<MenuItem> sortedList = new ArrayList<>(filteredItems);

        // Основной компаратор: сортировка по Категории (как и в админке)
        Comparator<MenuItem> categoryComparator = (item1, item2) -> {
            String category1 = item1.getCategory() != null ? item1.getCategory() : "Без категории";
            String category2 = item2.getCategory() != null ? item2.getCategory() : "Без категории";
            return category1.compareToIgnoreCase(category2);
        };

        // Вторичный компаратор: по порядку или по алфавиту
        Comparator<MenuItem> secondaryComparator;
        if (currentSortType == SortType.ORDER) {
            secondaryComparator = Comparator.comparingInt(MenuItem::getOrder);
        } else { // SortType.ALPHABETICAL
            secondaryComparator = Comparator.comparing(MenuItem::getName, String.CASE_INSENSITIVE_ORDER);
        }

        // Применяем оба компаратора
        sortedList.sort(categoryComparator.thenComparing(secondaryComparator));

        return sortedList;
    }


    // --- ⬅️ РЕАЛИЗАЦИЯ ИНТЕРФЕЙСА CARTACTIONLISTENER ---
    @Override
    public void onCartUpdated() {
        // Вызывается из UserMenuAdapter после добавления/удаления блюда
        updateCartIconState();
    }

    /**
     * Обновляет внешний вид иконки корзины в Toolbar в зависимости от ее содержимого.
     */
    private void updateCartIconState() {
        if (cartButton.getVisibility() != View.VISIBLE) {
            return; // Не обновляем, если кнопка скрыта настройками
        }

        // Проверяем, пуста ли корзина
        boolean isEmpty = CartManager.getInstance().isEmpty();

        // Визуальная индикация:
        if (isEmpty) {
            cartButton.setAlpha(0.6f); // Делаем иконку менее заметной (серой)
            // TODO: Если вы используете BadgeDrawable, здесь нужно его скрыть
        } else {
            cartButton.setAlpha(1.0f); // Полная видимость (активная)
            // TODO: Если вы используете BadgeDrawable, здесь нужно его показать и обновить счетчик
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(resetClickCountRunnable);
    }
}