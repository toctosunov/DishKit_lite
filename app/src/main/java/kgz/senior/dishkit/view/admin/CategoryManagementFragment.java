package kgz.senior.dishkit.view.admin;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.model.Category;
import kgz.senior.dishkit.model.MenuItem;
import kgz.senior.dishkit.viewmodel.AdminViewModel;

public class CategoryManagementFragment extends Fragment
        implements CategoryManagementAdapter.OnCategoryActionListener {

    private AdminViewModel adminViewModel;
    private CategoryManagementAdapter adapter;
    private List<Category> availableCategories = new ArrayList<>();

    public CategoryManagementFragment() {
        // Требуется пустой публичный конструктор
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.categoryManagementRecyclerView);
        ExtendedFloatingActionButton fabAddCategory = view.findViewById(R.id.fabAddCategory);

        // 1. Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализация адаптера с пустым списком и самим фрагментом в качестве слушателя
        adapter = new CategoryManagementAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // 2. Наблюдение за LiveData категорий
        adminViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            availableCategories.clear();
            availableCategories.addAll(categories);
            adapter.setCategories(categories);
        });

        // 3. Обработка кнопки "Добавить"
        fabAddCategory.setOnClickListener(v -> showAddEditCategoryDialog(null));

        // Подписка на уведомления об успехе/ошибке
        adminViewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(requireContext(), "Операция с категорией успешна!", Toast.LENGTH_SHORT).show();
            }
        });
        adminViewModel.getError().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), "Ошибка: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // -------------------------------------------------------------------------
    // МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ КАТЕГОРИЯМИ
    // -------------------------------------------------------------------------

    /**
     * Отображает диалог для добавления или редактирования категории/подкатегории.
     * @param category Категория для редактирования, или null для добавления.
     */
    public void showAddEditCategoryDialog(@Nullable Category category) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

        // Используем разметку dialog_add_edit_category.xml
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_edit_category, null);
        builder.setView(dialogView);

        builder.setTitle(category == null ? "Добавить категорию/подкатегорию" : "Редактировать категорию");

        final TextInputEditText categoryNameInput = dialogView.findViewById(R.id.categoryNameEditText);
        final AutoCompleteTextView parentCategoryAutoComplete = dialogView.findViewById(R.id.parentCategoryAutoComplete);

        // --- Настройка AutoCompleteTextView для выбора родительской категории ---
        // Фильтруем список, чтобы показать только основные категории (parentId == null)
        List<Category> mainCategories = availableCategories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId().isEmpty())
                .collect(Collectors.toList());

        // Список имен для адаптера: "Нет родителя" + имена основных категорий
        List<String> parentNames = new ArrayList<>();
        parentNames.add("Нет родителя (Основная категория)");
        parentNames.addAll(mainCategories.stream().map(Category::getName).collect(Collectors.toList()));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, parentNames);
        parentCategoryAutoComplete.setAdapter(adapter);

        // --- Заполнение данных для редактирования ---
        if (category != null) {
            categoryNameInput.setText(category.getName());

            // Если категория - подкатегория, находим имя ее родителя
            if (category.getParentId() != null && !category.getParentId().isEmpty()) {
                Optional<Category> parent = mainCategories.stream()
                        .filter(c -> Objects.equals(c.getId(), category.getParentId()))
                        .findFirst();

                parent.ifPresent(p -> parentCategoryAutoComplete.setText(p.getName(), false));
            } else {
                // Это основная категория
                parentCategoryAutoComplete.setText(parentNames.get(0), false);
            }
        } else {
            // Новая категория, по умолчанию "Нет родителя"
            parentCategoryAutoComplete.setText(parentNames.get(0), false);
        }

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = categoryNameInput.getText().toString().trim();
            String selectedParentName = parentCategoryAutoComplete.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Название категории не может быть пустым", Toast.LENGTH_SHORT).show();
                return;
            }

            // Определяем ID родителя
            String tempParentId = null; // Временная переменная
            if (!selectedParentName.equals(parentNames.get(0))) {
                Optional<Category> parent = mainCategories.stream()
                        .filter(c -> c.getName().equals(selectedParentName))
                        .findFirst();

                if (parent.isPresent()) {
                    tempParentId = parent.get().getId();
                } else {
                    Toast.makeText(requireContext(), "Некорректно выбрана родительская категория.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // <-- ИСПРАВЛЕНИЕ: Используем final переменную для лямбды
            final String finalParentId = tempParentId;

            // --- Проверка на дубликат (в рамках родителя) ---
            boolean categoryExists = availableCategories.stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(newName)
                            && Objects.equals(c.getParentId(), finalParentId) // <-- Используем finalParentId
                            && (category == null || !c.getId().equals(category.getId())));

            if (categoryExists) {
                Toast.makeText(requireContext(), "Категория с таким названием уже существует в этой группе.", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- Сохранение ---
            Category categoryToSave = category != null ? category : new Category();
            categoryToSave.setName(newName);
            categoryToSave.setParentId(finalParentId); // <-- Используем finalParentId

            if (category == null) {
                categoryToSave.setOrder(availableCategories.size());
                categoryToSave.setId(null);
            }

            // Ограничение: нельзя сделать родительскую категорию (которая имеет подкатегории) подкатегорией!
            if (category != null &&
                    (category.getParentId() == null || category.getParentId().isEmpty()) && // Текущая категория - основная
                    finalParentId != null) { // И мы пытаемся сделать ее подкатегорией

                boolean hasSubcategories = availableCategories.stream()
                        .anyMatch(c -> Objects.equals(c.getParentId(), category.getId()));

                if (hasSubcategories) {
                    Toast.makeText(requireContext(), "Нельзя перенести категорию, имеющую подкатегории, в подкатегорию.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            adminViewModel.addOrUpdateCategory(categoryToSave);
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.my_custom_dialog_background);
        }

        dialog.show();
    }

    // --- Реализация интерфейса OnCategoryActionListener (для кликов в адаптере) ---

    @Override
    public void onEditClick(Category category) {
        showAddEditCategoryDialog(category);
    }

    @Override
    public void onDeleteClick(Category category) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Удалить категорию?")
                .setMessage("Вы уверены, что хотите удалить категорию \"" + category.getName() + "\"? " +
                        "Все блюда, привязанные к этой категории, будут помечены как \"Без категории\". " +
                        (category.getParentId() == null || category.getParentId().isEmpty() ? "Будут удалены также все ее подкатегории." : "")) // Предупреждение о подкатегориях
                .setPositiveButton("Удалить", (dialog, which) -> {

                    // 1. Получаем список блюд, которые нужно обновить
                    List<MenuItem> itemsToUpdate = adminViewModel.getMenuItems().getValue() != null
                            ? adminViewModel.getMenuItems().getValue().stream()
                            .filter(item -> category.getName().equals(item.getCategory()))
                            .collect(Collectors.toList())
                            : new ArrayList<>();

                    // 2. Обновляем блюда асинхронно
                    for (MenuItem item : itemsToUpdate) {
                        item.setCategory("Без категории");
                        adminViewModel.addOrUpdateMenuItem(item, null);
                    }

                    // 3. Удаляем саму категорию (ViewModel позаботится о подкатегориях)
                    adminViewModel.deleteCategory(category.getId());
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}